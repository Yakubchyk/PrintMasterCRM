<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Склад: материалы, остатки, резерв под заказы, движение. */
class PM_Materials {

	// ---------- чтение ----------

	public static function by_category( $category ) {
		$t = PM_Db::t( 'materials' );
		return PM_Db::rows( "SELECT * FROM $t WHERE category = %s ORDER BY name ASC", array( $category ) ); // phpcs:ignore WordPress.DB.PreparedSQL
	}

	public static function get( $id ) {
		$t   = PM_Db::t( 'materials' );
		$row = PM_Db::row( "SELECT * FROM $t WHERE id = %d", array( (int) $id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
		if ( ! $row ) {
			throw new PM_Error( 'Материал не найден' );
		}
		return $row;
	}

	public static function movements( $material_id ) {
		$t = PM_Db::t( 'movements' );
		return PM_Db::rows( "SELECT * FROM $t WHERE material_id = %d ORDER BY id DESC", array( (int) $material_id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
	}

	public static function available( $m ) {
		return PM_Num::round( (float) $m->stock - (float) $m->reserved, 3 );
	}

	/** Площадь одного исходного листа (рулона), см²; null, если размер не задан. */
	public static function sheet_area( $m ) {
		if ( $m->sheet_width_cm === null || $m->sheet_length_cm === null ) {
			return null;
		}
		return (float) $m->sheet_width_cm * (float) $m->sheet_length_cm;
	}

	/** Намотка рулона фольги, м (в базе хранится длина в см). */
	public static function roll_length_m( $m ) {
		return $m->sheet_length_cm === null ? null : (float) $m->sheet_length_cm / 100;
	}

	public static function unit( $m ) {
		return PM_Units::cat_unit( $m->category );
	}

	// ---------- создание ----------

	private static function require_name( $name ) {
		$n = trim( sanitize_text_field( (string) $name ) );
		if ( $n === '' ) {
			throw new PM_Error( 'Укажите название материала' );
		}
		return $n;
	}

	private static function clean_note( $note ) {
		$n = trim( sanitize_text_field( (string) $note ) );
		return $n === '' ? null : $n;
	}

	private static function name_exists( $name, $exclude_id = 0 ) {
		$t    = PM_Db::t( 'materials' );
		$want = mb_strtolower( $name );
		foreach ( PM_Db::rows( "SELECT name FROM $t WHERE id <> %d", array( (int) $exclude_id ) ) as $r ) { // phpcs:ignore WordPress.DB.PreparedSQL
			if ( mb_strtolower( (string) $r->name ) === $want ) {
				return true;
			}
		}
		return false;
	}

	private static function whole( $v ) {
		return $v !== null && $v >= 0 && floor( $v ) == $v;
	}

	/** Бумага и картон: учёт в листах. */
	public static function create_paper( $name, $sheet_size, $initial_stock, $note ) {
		$n = self::require_name( $name );
		if ( self::name_exists( $n ) ) {
			throw new PM_Error( 'Материал с таким названием уже есть' );
		}
		$stock = $initial_stock === null ? 0.0 : $initial_stock;
		if ( ! self::whole( $stock ) ) {
			throw new PM_Error( 'Остаток бумаги — целое число листов, не меньше нуля' );
		}
		$size = trim( sanitize_text_field( (string) $sheet_size ) );
		return PM_Db::transaction(
			function () use ( $n, $size, $stock, $note ) {
				$id = PM_Db::insert(
					'materials',
					array(
						'name'       => $n,
						'category'   => 'PAPER',
						'sheet_size' => $size === '' ? null : $size,
						'note'       => self::clean_note( $note ),
						'stock'      => 0,
						'reserved'   => 0,
						'created_at' => PM_Db::now(),
					)
				);
				if ( $stock > 0 ) {
					self::receive( $id, $stock, 'Начальный остаток' );
				}
				return $id;
			}
		);
	}

	/** Магний/латунь: размер исходного листа (см) и число листов; остаток в см² = ширина × длина × листы. */
	public static function create_plate( $name, $category, $width_cm, $length_cm, $sheets, $note ) {
		if ( $category !== 'MAGNESIUM' && $category !== 'BRASS' ) {
			throw new PM_Error( 'Размер листа в см² задаётся только для магния и латуни' );
		}
		self::require_positive_size( $width_cm, $length_cm );
		$count = $sheets === null ? 0.0 : $sheets;
		if ( ! self::whole( $count ) ) {
			throw new PM_Error( 'Количество листов — целое число, не меньше нуля' );
		}
		$n = self::require_name( $name );
		if ( self::name_exists( $n ) ) {
			throw new PM_Error( 'Материал с таким названием уже есть' );
		}
		return PM_Db::transaction(
			function () use ( $n, $category, $width_cm, $length_cm, $count, $note ) {
				$id = PM_Db::insert(
					'materials',
					array(
						'name'            => $n,
						'category'        => $category,
						'sheet_width_cm'  => $width_cm,
						'sheet_length_cm' => $length_cm,
						'note'            => self::clean_note( $note ),
						'stock'           => 0,
						'reserved'        => 0,
						'created_at'      => PM_Db::now(),
					)
				);
				if ( $count > 0 ) {
					self::receive( $id, PM_Num::round( $width_cm * $length_cm * $count, 3 ), 'Начальный остаток: ' . PM_Num::plain( $count ) . ' лист.' );
				}
				return $id;
			}
		);
	}

	/** Фольга: тип, ширина рулона (см), намотка (м), число рулонов. Название собирается из типа и размера. */
	public static function create_foil( $foil_type, $width_cm, $winding_m, $rolls, $note ) {
		if ( ! isset( PM_Units::FOIL_TYPES[ $foil_type ] ) ) {
			throw new PM_Error( 'Выберите тип фольги' );
		}
		if ( $width_cm === null || $winding_m === null || $width_cm <= 0 || $winding_m <= 0 ) {
			throw new PM_Error( 'Ширина рулона и намотка должны быть больше нуля' );
		}
		$count = $rolls === null ? 0.0 : $rolls;
		if ( ! self::whole( $count ) ) {
			throw new PM_Error( 'Количество рулонов — целое число, не меньше нуля' );
		}
		$length_cm = PM_Num::round( $winding_m * 100, 3 );
		$name      = PM_Units::FOIL_TYPES[ $foil_type ] . ' ' . PM_Num::plain( $width_cm ) . ' см × ' . PM_Num::plain( $winding_m ) . ' м';
		if ( self::name_exists( $name ) ) {
			throw new PM_Error( 'Такая фольга («' . $name . '») уже есть: примите на неё рулоны на странице материала' );
		}
		return PM_Db::transaction(
			function () use ( $name, $foil_type, $width_cm, $length_cm, $count, $note ) {
				$id = PM_Db::insert(
					'materials',
					array(
						'name'            => $name,
						'category'        => 'FOIL',
						'foil_type'       => $foil_type,
						'sheet_width_cm'  => $width_cm,
						'sheet_length_cm' => $length_cm,
						'note'            => self::clean_note( $note ),
						'stock'           => 0,
						'reserved'        => 0,
						'created_at'      => PM_Db::now(),
					)
				);
				if ( $count > 0 ) {
					self::receive( $id, PM_Num::round( $width_cm * $length_cm * $count, 3 ), 'Начальный остаток: ' . PM_Num::plain( $count ) . ' рул.' );
				}
				return $id;
			}
		);
	}

	private static function require_positive_size( $w, $l ) {
		if ( $w === null || $l === null || $w <= 0 || $l <= 0 ) {
			throw new PM_Error( 'Ширина и длина листа должны быть больше нуля' );
		}
	}

	// ---------- изменение ----------

	/** Название, примечание, размер листа (бумага — текстом). Раздел не меняется: от него зависит единица учёта. */
	public static function update( $id, $name, $sheet_size, $note ) {
		$m = self::get( $id );
		$n = self::require_name( $name );
		if ( self::name_exists( $n, $id ) ) {
			throw new PM_Error( 'Материал с таким названием уже есть' );
		}
		$size = trim( sanitize_text_field( (string) $sheet_size ) );
		PM_Db::update(
			'materials',
			array(
				'name'       => $n,
				'sheet_size' => $m->category === 'PAPER' ? ( $size === '' ? null : $size ) : null,
				'note'       => self::clean_note( $note ),
			),
			array( 'id' => (int) $id )
		);
	}

	/** Размер исходного листа магния/латуни или рулона фольги (остаток не пересчитывается). */
	public static function update_size( $id, $width_cm, $length_cm ) {
		$m = self::get( $id );
		if ( $m->category === 'PAPER' ) {
			throw new PM_Error( 'Для бумаги размер листа задаётся текстом' );
		}
		self::require_positive_size( $width_cm, $length_cm );
		PM_Db::update( 'materials', array( 'sheet_width_cm' => $width_cm, 'sheet_length_cm' => $length_cm ), array( 'id' => (int) $id ) );
	}

	/** Приход целых листов (рулонов): на склад добавляется площадь. */
	public static function receive_sheets( $id, $sheets, $note ) {
		$m    = self::get( $id );
		$area = self::sheet_area( $m );
		if ( $area === null ) {
			throw new PM_Error( 'У материала «' . $m->name . '» не задан размер листа' );
		}
		if ( $sheets === null || $sheets <= 0 || floor( $sheets ) != $sheets ) {
			throw new PM_Error( 'Количество листов — целое число больше нуля' );
		}
		$text = PM_Num::plain( $sheets ) . ( $m->category === 'FOIL' ? ' рул.' : ' лист.' );
		$note = trim( (string) $note );
		self::receive( $id, PM_Num::round( $area * $sheets, 3 ), $note === '' ? $text : $text . ', ' . $note );
	}

	public static function receive( $id, $quantity, $note ) {
		self::move( $id, 'IN', $quantity, $note );
	}

	public static function consume( $id, $quantity, $note ) {
		self::move( $id, 'OUT', $quantity, $note );
	}

	/** Удаляет материал вместе с историей движения, если он не использован в заказах. */
	public static function delete( $id ) {
		$m  = self::get( $id );
		$om = PM_Db::t( 'order_materials' );
		if ( PM_Db::var_( "SELECT id FROM $om WHERE material_id = %d LIMIT 1", array( (int) $id ) ) ) { // phpcs:ignore WordPress.DB.PreparedSQL
			throw new PM_Error( 'Материал «' . $m->name . '» списывался на заказы, удалить его нельзя' );
		}
		PM_Db::transaction(
			function () use ( $id ) {
				PM_Db::query( 'UPDATE ' . PM_Db::t( 'cliche' ) . ' SET material_id = NULL WHERE material_id = %d', array( (int) $id ) );
				PM_Db::query( 'UPDATE ' . PM_Db::t( 'postpress' ) . ' SET material_id = NULL WHERE material_id = %d', array( (int) $id ) );
				PM_Db::delete( 'movements', array( 'material_id' => (int) $id ) );
				PM_Db::delete( 'materials', array( 'id' => (int) $id ) );
			}
		);
	}

	// ---------- резерв и списание под заказы ----------

	/** Резервирует материал под заказ: остаток не меняется, но свободного становится меньше. */
	public static function reserve( $id, $quantity ) {
		$m      = self::get( $id );
		$amount = self::normalize( $m, $quantity );
		if ( $amount > self::available( $m ) ) {
			throw self::not_enough( $m );
		}
		PM_Db::update( 'materials', array( 'reserved' => PM_Num::round( (float) $m->reserved + $amount, 3 ) ), array( 'id' => (int) $id ) );
	}

	/** Снимает резерв (заказ удалён до выполнения). */
	public static function release( $id, $quantity ) {
		$m = self::get( $id );
		PM_Db::update( 'materials', array( 'reserved' => max( 0.0, PM_Num::round( (float) $m->reserved - (float) $quantity, 3 ) ) ), array( 'id' => (int) $id ) );
	}

	/** Выполнение заказа: резерв превращается в списание со склада. */
	public static function ship( $id, $quantity, $note ) {
		$m      = self::get( $id );
		$amount = PM_Num::round( (float) $quantity, 3 );
		if ( $amount > (float) $m->stock ) {
			throw self::not_enough( $m );
		}
		PM_Db::update(
			'materials',
			array(
				'reserved' => max( 0.0, PM_Num::round( (float) $m->reserved - $amount, 3 ) ),
				'stock'    => PM_Num::round( (float) $m->stock - $amount, 3 ),
			),
			array( 'id' => (int) $id )
		);
		self::write_movement( $id, 'OUT', $amount, $note );
	}

	/** Отмена выполнения: материал возвращается на склад и снова в резерве заказа. */
	public static function unship( $id, $quantity, $note ) {
		$m      = self::get( $id );
		$amount = PM_Num::round( (float) $quantity, 3 );
		PM_Db::update(
			'materials',
			array(
				'stock'    => PM_Num::round( (float) $m->stock + $amount, 3 ),
				'reserved' => PM_Num::round( (float) $m->reserved + $amount, 3 ),
			),
			array( 'id' => (int) $id )
		);
		self::write_movement( $id, 'IN', $amount, $note );
	}

	// ---------- внутреннее ----------

	private static function move( $id, $type, $quantity, $note ) {
		$m          = self::get( $id );
		$amount     = self::normalize( $m, $quantity );
		$new_stock  = $type === 'IN' ? (float) $m->stock + $amount : (float) $m->stock - $amount;
		$new_stock  = PM_Num::round( $new_stock, 3 );
		// списать можно только свободный остаток: зарезервированное принадлежит заказам
		if ( $new_stock < 0 || $new_stock < (float) $m->reserved ) {
			throw self::not_enough( $m );
		}
		PM_Db::transaction(
			function () use ( $id, $new_stock, $type, $amount, $note ) {
				PM_Db::update( 'materials', array( 'stock' => $new_stock ), array( 'id' => (int) $id ) );
				self::write_movement( $id, $type, $amount, $note );
			}
		);
	}

	private static function write_movement( $id, $type, $amount, $note ) {
		$n = trim( sanitize_text_field( (string) $note ) );
		PM_Db::insert(
			'movements',
			array(
				'material_id' => (int) $id,
				'type'        => $type,
				'quantity'    => $amount,
				'note'        => $n === '' ? null : $n,
				'created_at'  => PM_Db::now(),
			)
		);
	}

	private static function normalize( $m, $quantity ) {
		if ( $quantity === null || $quantity <= 0 ) {
			throw new PM_Error( 'Количество должно быть больше нуля' );
		}
		if ( self::unit( $m )['whole'] && floor( $quantity ) != $quantity ) {
			throw new PM_Error( '«' . $m->name . '»: листы указываются целым числом' );
		}
		return PM_Num::round( (float) $quantity, 3 );
	}

	private static function not_enough( $m ) {
		$unit = self::unit( $m )['label'];
		$msg  = 'На складе недостаточно материала «' . $m->name . '»: свободно ' . PM_Num::qty( self::available( $m ) ) . ' ' . $unit;
		if ( (float) $m->reserved > 0 ) {
			$msg .= ' (в резерве ' . PM_Num::qty( $m->reserved ) . ')';
		}
		return new PM_Error( $msg );
	}
}
