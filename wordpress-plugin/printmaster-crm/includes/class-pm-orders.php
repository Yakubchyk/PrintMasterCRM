<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Заказы: оформление с резервом материалов, статусы, поиск, сводка по клиентам. */
class PM_Orders {

	// ---------- чтение ----------

	public static function get( $id ) {
		$t   = PM_Db::t( 'orders' );
		$row = PM_Db::row( "SELECT * FROM $t WHERE id = %d", array( (int) $id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
		if ( ! $row ) {
			throw new PM_Error( 'Заказ не найден' );
		}
		return $row;
	}

	/** Заказы клиента (или все, если $customer_id пуст), новые сверху. */
	public static function find( $customer_id = null ) {
		$t = PM_Db::t( 'orders' );
		if ( $customer_id ) {
			return PM_Db::rows( "SELECT * FROM $t WHERE customer_id = %d ORDER BY id DESC", array( (int) $customer_id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
		}
		return PM_Db::rows( "SELECT * FROM $t ORDER BY id DESC" ); // phpcs:ignore WordPress.DB.PreparedSQL
	}

	/** Материалы по заказам: id заказа -> список строк (с названием и разделом материала). */
	public static function materials_by_order( array $orders ) {
		$result = array();
		if ( ! $orders ) {
			return $result;
		}
		$ids = array_map(
			function ( $o ) {
				return (int) $o->id;
			},
			$orders
		);
		$om  = PM_Db::t( 'order_materials' );
		$m   = PM_Db::t( 'materials' );
		$in  = implode( ',', $ids ); // только целые числа
		$rows = PM_Db::rows(
			"SELECT om.*, m.name AS material_name, m.category AS material_category, m.sheet_size AS material_sheet_size
			 FROM $om om JOIN $m m ON m.id = om.material_id WHERE om.order_id IN ($in) ORDER BY om.id ASC" // phpcs:ignore WordPress.DB.PreparedSQL
		);
		foreach ( $rows as $r ) {
			$result[ (int) $r->order_id ][] = $r;
		}
		return $result;
	}

	/**
	 * Поиск: номер, клиент (название, телефон, email, город, улица), описание, состав работ
	 * и названия материалов. Пустой запрос возвращает все заказы.
	 */
	public static function search( $query ) {
		$all = self::find();
		$q   = trim( (string) $query );
		if ( $q === '' ) {
			return $all;
		}
		$needle    = mb_strtolower( $q );
		$by_order  = self::materials_by_order( $all );
		$customers = array();
		foreach ( PM_Customers::all() as $c ) {
			$customers[ (int) $c->id ] = $c;
		}
		$numeric = ctype_digit( $needle );
		$result  = array();
		foreach ( $all as $o ) {
			if ( $numeric && (string) $o->id === $needle ) {
				$result[] = $o;
				continue;
			}
			$c   = isset( $customers[ (int) $o->customer_id ] ) ? $customers[ (int) $o->customer_id ] : null;
			$hay = array( '№' . $o->id, $o->product, $o->details );
			if ( $c ) {
				array_push( $hay, $c->name, $c->phone, $c->email, $c->city, $c->street );
			}
			if ( isset( $by_order[ (int) $o->id ] ) ) {
				foreach ( $by_order[ (int) $o->id ] as $u ) {
					array_push( $hay, $u->material_name, $u->material_sheet_size );
				}
			}
			if ( mb_strpos( mb_strtolower( implode( "\n", array_filter( $hay ) ) ), $needle ) !== false ) {
				$result[] = $o;
			}
		}
		return $result;
	}

	/** По одной сводке на клиента; у кого заказы были недавно — выше. */
	public static function summaries( array $customers ) {
		$by_customer = array();
		foreach ( self::find() as $o ) {
			$by_customer[ (int) $o->customer_id ][] = $o;
		}
		$result = array();
		foreach ( $customers as $c ) {
			$orders = isset( $by_customer[ (int) $c->id ] ) ? $by_customer[ (int) $c->id ] : array();
			$total  = 0.0;
			$active = 0;
			foreach ( $orders as $o ) {
				$total += (float) $o->total_cost;
				if ( $o->status !== 'DONE' ) {
					$active++;
				}
			}
			$result[] = (object) array(
				'customer'      => $c,
				'order_count'   => count( $orders ),
				'active_count'  => $active,
				'total'         => $total,
				'last_order_at' => $orders ? $orders[0]->created_at : null,
			);
		}
		usort(
			$result,
			function ( $a, $b ) {
				if ( $a->last_order_at === $b->last_order_at ) {
					return 0;
				}
				if ( $a->last_order_at === null ) {
					return 1;
				}
				if ( $b->last_order_at === null ) {
					return -1;
				}
				return strcmp( $b->last_order_at, $a->last_order_at );
			}
		);
		return $result;
	}

	// ---------- оформление ----------

	/**
	 * Оформляет заказ из выбранных расчётов клиента. Суммы берутся из сохранённых расчётов, из запроса
	 * принимаются только их id и описание. Материалы резервируются: вручную указанные ($consumption:
	 * id материала -> количество), материал клише (площадь с полями × количество) и фольга тиснения
	 * (размер оттиска × тираж). Если свободного материала не хватает, заказ не создаётся.
	 * Со склада материал списывается, когда заказ получает статус «Выполнен».
	 */
	public static function create( $customer_id, $product, array $postpress_ids, array $printing_ids, array $cliche_ids, array $consumption ) {
		$customer = PM_Customers::get( $customer_id );
		$product  = trim( sanitize_text_field( (string) $product ) );
		if ( $product === '' ) {
			throw new PM_Error( 'Укажите детали заказа' );
		}
		$pp_ids = array_values( array_unique( array_map( 'intval', $postpress_ids ) ) );
		$pr_ids = array_values( array_unique( array_map( 'intval', $printing_ids ) ) );
		$cl_ids = array_values( array_unique( array_map( 'intval', $cliche_ids ) ) );
		if ( ! $pp_ids && ! $pr_ids && ! $cl_ids ) {
			throw new PM_Error( 'Выберите хотя бы один расчёт для заказа' );
		}
		$settings = PM_Settings::get();
		$lu       = $settings['length_unit'];
		$ul       = PM_Units::length_label( $lu );

		return PM_Db::transaction(
			function () use ( $customer, $product, $pp_ids, $pr_ids, $cl_ids, $consumption, $lu, $ul ) {
				$lines      = array();
				$to_reserve = array();
				foreach ( $consumption as $id => $qty ) {
					if ( $qty !== null && (float) $qty != 0.0 ) {
						$to_reserve[ (int) $id ] = ( isset( $to_reserve[ (int) $id ] ) ? $to_reserve[ (int) $id ] : 0.0 ) + (float) $qty;
					}
				}

				$cliche_sum = 0.0;
				foreach ( $cl_ids as $id ) {
					$cl = self::owned_calc( 'cliche', $id, $customer->id, 'Расчёт клише не найден' );
					$cliche_sum += (float) $cl->total_price;
					$mat = $cl->material_id ? PM_Materials::get( $cl->material_id ) : null;
					$line = 'Клише: ' . PM_Units::fmt_len( $cl->width_cm, $lu ) . ' × ' . PM_Units::fmt_len( $cl->length_cm, $lu ) . ' ' . $ul;
					if ( (float) $cl->margin_cm > 0 ) {
						$line .= ', поля ' . PM_Units::fmt_len( $cl->margin_cm, $lu ) . ' ' . $ul;
					}
					$line .= ', общая площадь с полями ' . PM_Num::plain( PM_Calcs::cliche_area( $cl ), 2 ) . ' см², ' . (int) $cl->quantity . ' шт.';
					if ( $mat ) {
						$line .= ', ' . $mat->name;
						$to_reserve[ (int) $mat->id ] = ( isset( $to_reserve[ (int) $mat->id ] ) ? $to_reserve[ (int) $mat->id ] : 0.0 )
							+ PM_Num::round( PM_Calcs::cliche_total_area( $cl ), 3 );
					}
					$lines[] = $line . ' — ' . self::money_plain( $cl->total_price );
				}

				$postpress_sum = 0.0;
				foreach ( $pp_ids as $id ) {
					$pp = self::owned_calc( 'postpress', $id, $customer->id, 'Расчёт тиснения не найден' );
					$postpress_sum += (float) $pp->total_price;
					$mat   = $pp->material_id ? PM_Materials::get( $pp->material_id ) : null;
					$lines[] = 'Тиснение: оттиск ' . PM_Units::fmt_len( $pp->width_cm, $lu ) . ' × ' . PM_Units::fmt_len( $pp->length_cm, $lu ) . ' ' . $ul
						. ', тираж ' . (int) $pp->quantity . ( $mat ? ', ' . $mat->name : '' ) . ' — ' . self::money_plain( $pp->total_price );
					if ( $mat ) {
						$to_reserve[ (int) $mat->id ] = ( isset( $to_reserve[ (int) $mat->id ] ) ? $to_reserve[ (int) $mat->id ] : 0.0 )
							+ PM_Num::round( PM_Calcs::postpress_foil_area( $pp ), 3 );
					}
				}

				$printing_sum = 0.0;
				foreach ( $pr_ids as $id ) {
					$pr = self::owned_calc( 'printing', $id, $customer->id, 'Расчёт печати не найден' );
					$printing_sum += (float) $pr->total_cost;
					$lines[] = 'Печать: бумага ' . $pr->papier . ', ' . $pr->colour . ', тираж ' . (int) $pr->quantity . ' — ' . self::money_plain( $pr->total_cost );
				}

				$order_id = PM_Db::insert(
					'orders',
					array(
						'customer_id'     => (int) $customer->id,
						'product'         => mb_substr( $product, 0, 255 ),
						'details'         => implode( "\n", $lines ),
						'postpress_price' => PM_Num::round( $postpress_sum, 2 ),
						'printing_price'  => PM_Num::round( $printing_sum, 2 ),
						'cliche_price'    => PM_Num::round( $cliche_sum, 2 ),
						'total_cost'      => PM_Num::round( $postpress_sum + $printing_sum + $cliche_sum, 2 ),
						'status'          => 'NEW',
						'created_at'      => PM_Db::now(),
					)
				);

				foreach ( $to_reserve as $material_id => $quantity ) {
					PM_Materials::reserve( $material_id, $quantity );
					PM_Db::insert(
						'order_materials',
						array(
							'order_id'    => $order_id,
							'material_id' => (int) $material_id,
							'quantity'    => PM_Num::round( $quantity, 3 ),
							'written_off' => 0,
						)
					);
				}
				return $order_id;
			}
		);
	}

	private static function money_plain( $v ) {
		return number_format( (float) $v, 2, '.', '' );
	}

	/** Расчёт должен существовать и принадлежать этому клиенту. */
	private static function owned_calc( $table, $id, $customer_id, $not_found ) {
		$t   = PM_Db::t( $table );
		$row = PM_Db::row( "SELECT * FROM $t WHERE id = %d", array( (int) $id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
		if ( ! $row || (int) $row->customer_id !== (int) $customer_id ) {
			throw new PM_Error( $not_found );
		}
		return $row;
	}

	// ---------- статус ----------

	/** Статус «Выполнен» списывает зарезервированные материалы со склада; возврат из него снова ставит их в резерв. */
	public static function set_status( $order_id, $status ) {
		if ( ! isset( PM_Units::STATUSES[ $status ] ) ) {
			throw new PM_Error( 'Неизвестный статус заказа' );
		}
		$order = self::get( $order_id );
		$old   = $order->status;
		PM_Db::transaction(
			function () use ( $order_id, $status, $old ) {
				$om   = PM_Db::t( 'order_materials' );
				$used = PM_Db::rows( "SELECT * FROM $om WHERE order_id = %d", array( (int) $order_id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
				if ( $old !== 'DONE' && $status === 'DONE' ) {
					foreach ( $used as $u ) {
						if ( ! (int) $u->written_off ) {
							PM_Materials::ship( $u->material_id, $u->quantity, 'Заказ №' . $order_id . ' выполнен' );
							PM_Db::update( 'order_materials', array( 'written_off' => 1 ), array( 'id' => (int) $u->id ) );
						}
					}
				} elseif ( $old === 'DONE' && $status !== 'DONE' ) {
					foreach ( $used as $u ) {
						if ( (int) $u->written_off ) {
							PM_Materials::unship( $u->material_id, $u->quantity, 'Отмена выполнения: заказ №' . $order_id );
							PM_Db::update( 'order_materials', array( 'written_off' => 0 ), array( 'id' => (int) $u->id ) );
						}
					}
				}
				$completed = null;
				if ( $status === 'DONE' ) {
					$completed = ( $old !== 'DONE' ) ? PM_Db::now() : null;
				}
				$data = array( 'status' => $status );
				if ( $status === 'DONE' && $old !== 'DONE' ) {
					$data['completed_at'] = $completed;
				} elseif ( $status !== 'DONE' ) {
					$data['completed_at'] = null;
				}
				PM_Db::update( 'orders', $data, array( 'id' => (int) $order_id ) );
			}
		);
	}

	// ---------- удаление ----------

	/** Удаляет заказ; резерв снимается, а уже списанные материалы возвращаются на склад. */
	public static function delete( $order_id ) {
		$order = self::get( $order_id );
		PM_Db::transaction(
			function () use ( $order ) {
				self::remove_with_stock_return( $order );
			}
		);
	}

	public static function delete_by_customer( $customer_id ) {
		foreach ( self::find( $customer_id ) as $order ) {
			self::remove_with_stock_return( $order );
		}
	}

	private static function remove_with_stock_return( $order ) {
		$om   = PM_Db::t( 'order_materials' );
		$used = PM_Db::rows( "SELECT * FROM $om WHERE order_id = %d", array( (int) $order->id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
		foreach ( $used as $u ) {
			if ( (int) $u->written_off ) {
				PM_Materials::receive( $u->material_id, (float) $u->quantity, 'Возврат: заказ №' . $order->id . ' удалён' );
			} else {
				PM_Materials::release( $u->material_id, $u->quantity );
			}
		}
		PM_Db::delete( 'order_materials', array( 'order_id' => (int) $order->id ) );
		PM_Db::delete( 'orders', array( 'id' => (int) $order->id ) );
	}
}
