<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Клиенты. */
class PM_Customers {

	/** Все клиенты по алфавиту; с запросом — только подходящие по названию, телефону или email (без учёта регистра). */
	public static function all( $query = '' ) {
		$t    = PM_Db::t( 'customers' );
		$rows = PM_Db::rows( "SELECT * FROM $t ORDER BY name ASC" ); // phpcs:ignore WordPress.DB.PreparedSQL
		$q    = trim( (string) $query );
		if ( $q === '' ) {
			return $rows;
		}
		$res = array();
		foreach ( $rows as $c ) {
			if ( mb_stripos( $c->name, $q ) !== false || mb_stripos( $c->phone, $q ) !== false || mb_stripos( $c->email, $q ) !== false ) {
				$res[] = $c;
			}
		}
		return $res;
	}

	public static function get( $id ) {
		$t   = PM_Db::t( 'customers' );
		$row = PM_Db::row( "SELECT * FROM $t WHERE id = %d", array( (int) $id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
		if ( ! $row ) {
			throw new PM_Error( 'Клиент не найден' );
		}
		return $row;
	}

	/** Адрес одной строкой: «улица дом, индекс город»; пустые части пропускаются. */
	public static function address_line( $c ) {
		$line1 = self::join( ' ', array( $c->street, $c->house_number ) );
		$line2 = self::join( ' ', array( $c->postal_code, $c->city ) );
		return self::join( ', ', array( $line1, $line2 ) );
	}

	private static function join( $sep, array $parts ) {
		$out = array();
		foreach ( $parts as $p ) {
			if ( $p !== null && trim( (string) $p ) !== '' ) {
				$out[] = trim( $p );
			}
		}
		return implode( $sep, $out );
	}

	/** Поля формы -> очищенные значения. */
	private static function clean( array $f ) {
		$g = function ( $key ) use ( $f ) {
			return isset( $f[ $key ] ) ? trim( sanitize_text_field( $f[ $key ] ) ) : '';
		};
		$data = array(
			'name'         => $g( 'name' ),
			'phone'        => $g( 'phone' ),
			'email'        => $g( 'email' ),
			'postal_code'  => $g( 'postal_code' ),
			'city'         => $g( 'city' ),
			'street'       => $g( 'street' ),
			'house_number' => $g( 'house_number' ),
		);
		if ( $data['name'] === '' || $data['phone'] === '' || $data['email'] === '' ) {
			throw new PM_Error( 'Заполните название организации, телефон и email' );
		}
		if ( ! is_email( $data['email'] ) ) {
			throw new PM_Error( 'Проверьте email: адрес записан неверно' );
		}
		foreach ( array( 'postal_code', 'city', 'street', 'house_number' ) as $k ) {
			$data[ $k ] = $data[ $k ] === '' ? null : $data[ $k ];
		}
		return $data;
	}

	/** Название, телефон и email клиента не должны повторяться (без учёта регистра). */
	private static function assert_unique( array $data, $exclude_id ) {
		$t      = PM_Db::t( 'customers' );
		$others = PM_Db::rows( "SELECT id, name, phone, email FROM $t WHERE id <> %d", array( (int) $exclude_id ) ); // phpcs:ignore WordPress.DB.PreparedSQL
		$checks = array(
			'name'  => 'Клиент с таким названием уже существует',
			'phone' => 'Клиент с таким телефоном уже существует',
			'email' => 'Клиент с таким email уже существует',
		);
		foreach ( $checks as $col => $msg ) {
			$want = mb_strtolower( $data[ $col ] );
			foreach ( $others as $o ) {
				if ( mb_strtolower( (string) $o->$col ) === $want ) {
					throw new PM_Error( $msg );
				}
			}
		}
	}

	public static function create( array $form ) {
		$data = self::clean( $form );
		self::assert_unique( $data, 0 );
		$data['created_at'] = PM_Db::now();
		return PM_Db::insert( 'customers', $data );
	}

	public static function update( $id, array $form ) {
		self::get( $id );
		$data = self::clean( $form );
		self::assert_unique( $data, (int) $id );
		PM_Db::update( 'customers', $data, array( 'id' => (int) $id ) );
	}

	/** Удаляет клиента вместе с его заказами и расчётами (материалы из заказов возвращаются на склад). */
	public static function delete( $id ) {
		self::get( $id );
		PM_Db::transaction(
			function () use ( $id ) {
				PM_Orders::delete_by_customer( $id );
				PM_Db::delete( 'postpress', array( 'customer_id' => (int) $id ) );
				PM_Db::delete( 'printing', array( 'customer_id' => (int) $id ) );
				PM_Db::delete( 'cliche', array( 'customer_id' => (int) $id ) );
				PM_Db::delete( 'customers', array( 'id' => (int) $id ) );
			}
		);
	}
}
