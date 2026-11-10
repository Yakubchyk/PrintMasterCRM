<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Таблицы, установка, транзакции и обёртки над $wpdb с проверкой ошибок. */
class PM_Db {

	const VERSION = '1';
	const CAP     = 'pm_manage';

	private static $depth = 0;

	public static function t( $name ) {
		global $wpdb;
		return $wpdb->prefix . 'pm_' . $name;
	}

	public static function activate() {
		self::install();
		$role = get_role( 'administrator' );
		if ( $role ) {
			$role->add_cap( self::CAP );
		}
	}

	public static function maybe_upgrade() {
		if ( get_option( 'pm_db_version' ) !== self::VERSION ) {
			self::install();
		}
		// право доступа есть только у администраторов
		$role = get_role( 'administrator' );
		if ( $role && ! $role->has_cap( self::CAP ) ) {
			$role->add_cap( self::CAP );
		}
	}

	public static function install() {
		global $wpdb;
		require_once ABSPATH . 'wp-admin/includes/upgrade.php';
		$c = $wpdb->get_charset_collate();

		$sql = array();

		$sql[] = 'CREATE TABLE ' . self::t( 'customers' ) . " (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  name varchar(255) NOT NULL,
  phone varchar(100) NOT NULL,
  email varchar(255) NOT NULL,
  postal_code varchar(20) DEFAULT NULL,
  city varchar(100) DEFAULT NULL,
  street varchar(150) DEFAULT NULL,
  house_number varchar(20) DEFAULT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY  (id),
  KEY name (name(100))
) $c;";

		$sql[] = 'CREATE TABLE ' . self::t( 'materials' ) . " (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  name varchar(255) NOT NULL,
  category varchar(20) NOT NULL,
  sheet_size varchar(50) DEFAULT NULL,
  sheet_width_cm decimal(12,3) DEFAULT NULL,
  sheet_length_cm decimal(12,3) DEFAULT NULL,
  foil_type varchar(20) DEFAULT NULL,
  stock decimal(18,3) NOT NULL DEFAULT 0,
  reserved decimal(18,3) NOT NULL DEFAULT 0,
  note varchar(255) DEFAULT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY  (id),
  KEY category (category)
) $c;";

		$sql[] = 'CREATE TABLE ' . self::t( 'movements' ) . " (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  material_id bigint(20) unsigned NOT NULL,
  type varchar(3) NOT NULL,
  quantity decimal(18,3) NOT NULL,
  note varchar(255) DEFAULT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY  (id),
  KEY material_id (material_id)
) $c;";

		$sql[] = 'CREATE TABLE ' . self::t( 'postpress' ) . " (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  customer_id bigint(20) unsigned NOT NULL,
  material_id bigint(20) unsigned DEFAULT NULL,
  width_cm decimal(12,3) NOT NULL,
  length_cm decimal(12,3) NOT NULL,
  quantity int(11) NOT NULL,
  total_price decimal(14,2) NOT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY  (id),
  KEY customer_id (customer_id)
) $c;";

		$sql[] = 'CREATE TABLE ' . self::t( 'printing' ) . " (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  customer_id bigint(20) unsigned NOT NULL,
  papier varchar(20) NOT NULL,
  colour varchar(10) NOT NULL,
  quantity int(11) NOT NULL,
  total_cost decimal(14,2) NOT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY  (id),
  KEY customer_id (customer_id)
) $c;";

		$sql[] = 'CREATE TABLE ' . self::t( 'cliche' ) . " (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  customer_id bigint(20) unsigned NOT NULL,
  material_id bigint(20) unsigned DEFAULT NULL,
  width_cm decimal(12,3) NOT NULL,
  length_cm decimal(12,3) NOT NULL,
  margin_cm decimal(12,3) NOT NULL DEFAULT 0,
  quantity int(11) NOT NULL,
  total_price decimal(14,2) NOT NULL,
  created_at datetime NOT NULL,
  PRIMARY KEY  (id),
  KEY customer_id (customer_id)
) $c;";

		$sql[] = 'CREATE TABLE ' . self::t( 'orders' ) . " (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  customer_id bigint(20) unsigned NOT NULL,
  product varchar(255) NOT NULL,
  details text DEFAULT NULL,
  postpress_price decimal(14,2) NOT NULL DEFAULT 0,
  printing_price decimal(14,2) NOT NULL DEFAULT 0,
  cliche_price decimal(14,2) NOT NULL DEFAULT 0,
  total_cost decimal(14,2) NOT NULL DEFAULT 0,
  status varchar(20) NOT NULL DEFAULT 'NEW',
  created_at datetime NOT NULL,
  completed_at datetime DEFAULT NULL,
  PRIMARY KEY  (id),
  KEY customer_id (customer_id)
) $c;";

		$sql[] = 'CREATE TABLE ' . self::t( 'order_materials' ) . " (
  id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  order_id bigint(20) unsigned NOT NULL,
  material_id bigint(20) unsigned NOT NULL,
  quantity decimal(18,3) NOT NULL,
  written_off tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY  (id),
  KEY order_id (order_id),
  KEY material_id (material_id)
) $c;";

		foreach ( $sql as $statement ) {
			dbDelta( $statement );
		}
		update_option( 'pm_db_version', self::VERSION );
	}

	// ---------- запросы с проверкой ошибок ----------

	private static function check( $result ) {
		global $wpdb;
		if ( $result === false ) {
			error_log( 'PrintMaster CRM DB error: ' . $wpdb->last_error ); // phpcs:ignore WordPress.PHP.DevelopmentFunctions
			throw new PM_Error( 'Ошибка базы данных. Подробности записаны в журнал ошибок сайта.' );
		}
		return $result;
	}

	public static function insert( $table, array $data ) {
		global $wpdb;
		self::check( $wpdb->insert( self::t( $table ), $data ) );
		return (int) $wpdb->insert_id;
	}

	public static function update( $table, array $data, array $where ) {
		global $wpdb;
		return self::check( $wpdb->update( self::t( $table ), $data, $where ) );
	}

	public static function delete( $table, array $where ) {
		global $wpdb;
		return self::check( $wpdb->delete( self::t( $table ), $where ) );
	}

	/** Выполняет подготовленный запрос: query( 'UPDATE ... WHERE id = %d', array( 5 ) ). */
	public static function query( $sql, array $args = array() ) {
		global $wpdb;
		$prepared = $args ? $wpdb->prepare( $sql, $args ) : $sql; // phpcs:ignore WordPress.DB.PreparedSQL
		return self::check( $wpdb->query( $prepared ) ); // phpcs:ignore WordPress.DB.PreparedSQL
	}

	public static function row( $sql, array $args = array() ) {
		global $wpdb;
		$prepared = $args ? $wpdb->prepare( $sql, $args ) : $sql; // phpcs:ignore WordPress.DB.PreparedSQL
		$row      = $wpdb->get_row( $prepared ); // phpcs:ignore WordPress.DB.PreparedSQL
		if ( $wpdb->last_error ) {
			self::check( false );
		}
		return $row;
	}

	public static function rows( $sql, array $args = array() ) {
		global $wpdb;
		$prepared = $args ? $wpdb->prepare( $sql, $args ) : $sql; // phpcs:ignore WordPress.DB.PreparedSQL
		$rows     = $wpdb->get_results( $prepared ); // phpcs:ignore WordPress.DB.PreparedSQL
		if ( $wpdb->last_error ) {
			self::check( false );
		}
		return $rows ? $rows : array();
	}

	public static function var_( $sql, array $args = array() ) {
		global $wpdb;
		$prepared = $args ? $wpdb->prepare( $sql, $args ) : $sql; // phpcs:ignore WordPress.DB.PreparedSQL
		$val      = $wpdb->get_var( $prepared ); // phpcs:ignore WordPress.DB.PreparedSQL
		if ( $wpdb->last_error ) {
			self::check( false );
		}
		return $val;
	}

	/** Время в часовом поясе сайта: «2026-11-10 18:45:00». */
	public static function now() {
		return current_time( 'mysql' );
	}

	/** Всё внутри — одна транзакция: при исключении откатывается целиком. Вложенные вызовы входят в внешнюю. */
	public static function transaction( $fn ) {
		global $wpdb;
		if ( self::$depth === 0 ) {
			$wpdb->query( 'START TRANSACTION' );
		}
		self::$depth++;
		try {
			$result = $fn();
		} catch ( Exception $e ) {
			self::$depth--;
			if ( self::$depth === 0 ) {
				$wpdb->query( 'ROLLBACK' );
			}
			throw $e;
		}
		self::$depth--;
		if ( self::$depth === 0 ) {
			$wpdb->query( 'COMMIT' );
		}
		return $result;
	}
}
