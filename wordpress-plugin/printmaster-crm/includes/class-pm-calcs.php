<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Сохранённые расчёты клиента: клише, тиснение, печать. Размеры хранятся в сантиметрах. */
class PM_Calcs {

	// ---------- клише ----------

	public static function cliche_list( $customer_id ) {
		$t = PM_Db::t( 'cliche' );
		return PM_Db::rows( "SELECT * FROM $t WHERE customer_id = %d ORDER BY id DESC", array( (int) $customer_id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
	}

	/** Размеры — рабочее поле; технологические поля добавляются с каждой стороны, цена считается по общему размеру. */
	public static function add_cliche( $customer_id, $width_cm, $length_cm, $margin_cm, $quantity, $material_id ) {
		PM_Customers::get( $customer_id );
		if ( $width_cm === null || $length_cm === null || $width_cm <= 0 || $length_cm <= 0 || $quantity === null || $quantity <= 0 ) {
			throw new PM_Error( 'Ширина, длина и количество должны быть больше нуля' );
		}
		$margin = $margin_cm === null ? 0.0 : $margin_cm;
		if ( $margin < 0 ) {
			throw new PM_Error( 'Технологические поля не могут быть отрицательными' );
		}
		if ( $material_id ) {
			$m = PM_Materials::get( $material_id );
			if ( $m->category !== 'MAGNESIUM' && $m->category !== 'BRASS' ) {
				throw new PM_Error( 'Для клише выберите магний или латунь' );
			}
		}
		$total = PM_Calc::cliche(
			PM_Calc::with_margins( $width_cm, $margin ),
			PM_Calc::with_margins( $length_cm, $margin ),
			$quantity,
			PM_Settings::get()
		);
		return PM_Db::insert(
			'cliche',
			array(
				'customer_id' => (int) $customer_id,
				'material_id' => $material_id ? (int) $material_id : null,
				'width_cm'    => $width_cm,
				'length_cm'   => $length_cm,
				'margin_cm'   => $margin,
				'quantity'    => (int) $quantity,
				'total_price' => $total,
				'created_at'  => PM_Db::now(),
			)
		);
	}

	public static function cliche_total_width( $c ) {
		return PM_Calc::with_margins( (float) $c->width_cm, (float) $c->margin_cm );
	}

	public static function cliche_total_length( $c ) {
		return PM_Calc::with_margins( (float) $c->length_cm, (float) $c->margin_cm );
	}

	/** Площадь одного клише с полями, см²: сначала к размерам прибавляются поля, затем они перемножаются. */
	public static function cliche_area( $c ) {
		return self::cliche_total_width( $c ) * self::cliche_total_length( $c );
	}

	/** Площадь всех клише расчёта с полями и количеством, см². */
	public static function cliche_total_area( $c ) {
		return self::cliche_area( $c ) * (int) $c->quantity;
	}

	// ---------- тиснение ----------

	public static function postpress_list( $customer_id ) {
		$t = PM_Db::t( 'postpress' );
		return PM_Db::rows( "SELECT * FROM $t WHERE customer_id = %d ORDER BY id DESC", array( (int) $customer_id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
	}

	public static function add_postpress( $customer_id, $width_cm, $length_cm, $quantity, $material_id ) {
		PM_Customers::get( $customer_id );
		if ( $width_cm === null || $length_cm === null || $width_cm <= 0 || $length_cm <= 0 || $quantity === null || $quantity <= 0 ) {
			throw new PM_Error( 'Ширина, длина и тираж должны быть больше нуля' );
		}
		if ( $material_id ) {
			$m = PM_Materials::get( $material_id );
			if ( $m->category !== 'FOIL' ) {
				throw new PM_Error( 'Для тиснения выберите материал из раздела «Фольга»' );
			}
		}
		return PM_Db::insert(
			'postpress',
			array(
				'customer_id' => (int) $customer_id,
				'material_id' => $material_id ? (int) $material_id : null,
				'width_cm'    => $width_cm,
				'length_cm'   => $length_cm,
				'quantity'    => (int) $quantity,
				'total_price' => PM_Calc::postpress( $width_cm, $length_cm, $quantity, PM_Settings::get() ),
				'created_at'  => PM_Db::now(),
			)
		);
	}

	/** Площадь фольги на весь тираж: размер оттиска × тираж, см². */
	public static function postpress_foil_area( $p ) {
		return (float) $p->width_cm * (float) $p->length_cm * (int) $p->quantity;
	}

	// ---------- печать ----------

	public static function printing_list( $customer_id ) {
		$t = PM_Db::t( 'printing' );
		return PM_Db::rows( "SELECT * FROM $t WHERE customer_id = %d ORDER BY id DESC", array( (int) $customer_id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
	}

	public static function add_printing( $customer_id, $papier, $colour, $quantity ) {
		PM_Customers::get( $customer_id );
		if ( $quantity === null || $quantity <= 0 ) {
			throw new PM_Error( 'Тираж должен быть больше нуля' );
		}
		if ( ! in_array( $papier, PM_Units::PAPERS, true ) || ! in_array( $colour, PM_Units::COLOURS, true ) ) {
			throw new PM_Error( 'Выберите бумагу и цветность из списка' );
		}
		return PM_Db::insert(
			'printing',
			array(
				'customer_id' => (int) $customer_id,
				'papier'      => $papier,
				'colour'      => $colour,
				'quantity'    => (int) $quantity,
				'total_cost'  => PM_Calc::printing( $papier, $colour, $quantity, PM_Settings::get() ),
				'created_at'  => PM_Db::now(),
			)
		);
	}

	// ---------- удаление (расчёт должен принадлежать клиенту) ----------

	public static function delete( $kind, $customer_id, $id ) {
		$tables = array( 'cliche' => 'cliche', 'postpress' => 'postpress', 'printing' => 'printing' );
		if ( ! isset( $tables[ $kind ] ) ) {
			throw new PM_Error( 'Расчёт не найден' );
		}
		$t     = PM_Db::t( $tables[ $kind ] );
		$found = PM_Db::var_( "SELECT id FROM $t WHERE id = %d AND customer_id = %d", array( (int) $id, (int) $customer_id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
		if ( ! $found ) {
			throw new PM_Error( 'Расчёт не найден' );
		}
		PM_Db::delete( $tables[ $kind ], array( 'id' => (int) $id ) );
	}
}
