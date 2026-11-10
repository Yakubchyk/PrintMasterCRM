<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Админка: меню, страницы, обработчики форм (admin-post), сообщения. Доступ только с правом pm_manage. */
class PM_Admin {

	const PAGES = array(
		'pm'            => array( 'Главная', 'home' ),
		'pm-customers'  => array( 'Клиенты', 'customers' ),
		'pm-orders'     => array( 'Заказы', 'orders' ),
		'pm-new-order'  => array( 'Новый заказ', 'order-new' ),
		'pm-materials'  => array( 'Материалы', 'materials' ),
		'pm-settings'   => array( 'Настройки', 'settings' ),
	);

	const ACTIONS = array(
		'customer_create', 'customer_update', 'customer_delete',
		'calc_add', 'calc_delete',
		'order_create', 'order_status', 'order_delete',
		'material_create', 'material_update', 'material_receive', 'material_receive_sheets', 'material_consume', 'material_delete',
		'settings_save',
	);

	public static function init() {
		add_action( 'admin_menu', array( __CLASS__, 'menu' ) );
		add_action( 'admin_enqueue_scripts', array( __CLASS__, 'assets' ) );
		add_filter( 'admin_body_class', array( __CLASS__, 'body_class' ) );
		foreach ( self::ACTIONS as $action ) {
			add_action( 'admin_post_pm_' . $action, array( __CLASS__, 'handle' ) );
		}
		add_action( 'admin_post_pm_print', array( __CLASS__, 'print_card' ) );
	}

	// ---------- меню и ресурсы ----------

	public static function menu() {
		add_menu_page( 'PrintMaster CRM', 'PrintMaster', PM_Db::CAP, 'pm', array( __CLASS__, 'render_page' ), 'dashicons-portfolio', 30 );
		foreach ( self::PAGES as $slug => $info ) {
			add_submenu_page( 'pm', $info[0] . ' — PrintMaster', $info[0], PM_Db::CAP, $slug, array( __CLASS__, 'render_page' ) );
		}
	}

	public static function is_pm_screen() {
		$page = isset( $_GET['page'] ) ? sanitize_key( wp_unslash( $_GET['page'] ) ) : ''; // phpcs:ignore WordPress.Security.NonceVerification
		return isset( self::PAGES[ $page ] );
	}

	public static function body_class( $classes ) {
		return self::is_pm_screen() ? $classes . ' pm-screen' : $classes;
	}

	public static function assets() {
		if ( ! self::is_pm_screen() ) {
			return;
		}
		wp_enqueue_style( 'pm-admin', PM_URL . 'assets/pm.css', array(), PM_VERSION );
		wp_enqueue_script( 'pm-admin', PM_URL . 'assets/pm.js', array(), PM_VERSION, true );
	}

	// ---------- сообщения ----------

	private static function flash_key() {
		return 'pm_flash_' . get_current_user_id();
	}

	public static function flash( $type, $message ) {
		set_transient( self::flash_key(), array( 'type' => $type, 'msg' => $message ), 120 );
	}

	/** Забирает (и стирает) сообщение для показа. */
	public static function take_flash() {
		$f = get_transient( self::flash_key() );
		if ( $f ) {
			delete_transient( self::flash_key() );
		}
		return $f ? $f : null;
	}

	// ---------- страницы ----------

	public static function render_page() {
		if ( ! current_user_can( PM_Db::CAP ) ) {
			wp_die( esc_html__( 'Недостаточно прав.', 'printmaster-crm' ), 403 );
		}
		$page = isset( $_GET['page'] ) ? sanitize_key( wp_unslash( $_GET['page'] ) ) : 'pm'; // phpcs:ignore WordPress.Security.NonceVerification
		if ( ! isset( self::PAGES[ $page ] ) ) {
			$page = 'pm';
		}
		try {
			PM_View::render( self::PAGES[ $page ][1], $page );
		} catch ( PM_Error $e ) {
			PM_View::render_error( $e->getMessage(), $page );
		}
	}

	// ---------- обработчики форм ----------

	public static function handle() {
		$action = isset( $_POST['action'] ) ? sanitize_key( wp_unslash( $_POST['action'] ) ) : ''; // phpcs:ignore WordPress.Security.NonceVerification
		if ( ! current_user_can( PM_Db::CAP ) ) {
			wp_die( esc_html__( 'Недостаточно прав.', 'printmaster-crm' ), 403 );
		}
		check_admin_referer( 'pm_action', 'pm_nonce' );
		$name = preg_replace( '/^pm_/', '', $action );
		if ( ! in_array( $name, self::ACTIONS, true ) ) {
			wp_die( 'Неизвестное действие', 400 );
		}
		try {
			$result = call_user_func( array( __CLASS__, 'do_' . $name ), wp_unslash( $_POST ) ); // phpcs:ignore WordPress.Security.NonceVerification
			if ( is_array( $result ) && ! empty( $result['ok'] ) ) {
				self::flash( 'ok', $result['ok'] );
				$url = $result['url'];
			} else {
				$url = $result;
			}
		} catch ( PM_Error $e ) {
			self::flash( 'error', $e->getMessage() );
			$url = wp_get_referer();
			if ( ! $url ) {
				$url = PM_View::url( 'pm' );
			}
		}
		wp_safe_redirect( $url );
		exit;
	}

	private static function val( array $p, $key ) {
		return isset( $p[ $key ] ) && is_string( $p[ $key ] ) ? $p[ $key ] : '';
	}

	/** Размер из формы в выбранных единицах -> см; null, если поле пустое. */
	private static function size_cm( array $p, $key ) {
		$v = PM_Num::parse( self::val( $p, $key ) );
		if ( $v === null ) {
			return null;
		}
		$lu = PM_Settings::get()['length_unit'];
		return PM_Units::to_cm( $v, $lu );
	}

	private static function do_customer_create( array $p ) {
		$id = PM_Customers::create( $p );
		return array( 'ok' => 'Клиент добавлен', 'url' => PM_View::url( 'pm-customers', array( 'id' => $id ) ) );
	}

	private static function do_customer_update( array $p ) {
		$id = (int) self::val( $p, 'id' );
		PM_Customers::update( $id, $p );
		return array( 'ok' => 'Данные клиента сохранены', 'url' => PM_View::url( 'pm-customers', array( 'id' => $id ) ) );
	}

	private static function do_customer_delete( array $p ) {
		PM_Customers::delete( (int) self::val( $p, 'id' ) );
		return array( 'ok' => 'Клиент удалён', 'url' => PM_View::url( 'pm-customers' ) );
	}

	private static function do_calc_add( array $p ) {
		$cid  = (int) self::val( $p, 'customer_id' );
		$kind = self::val( $p, 'kind' );
		$mat  = (int) self::val( $p, 'material_id' );
		$qty  = PM_Num::parse_int( self::val( $p, 'quantity' ) );
		if ( $kind === 'cliche' ) {
			$margin = self::size_cm( $p, 'margin' );
			PM_Calcs::add_cliche( $cid, self::size_cm( $p, 'width' ), self::size_cm( $p, 'length' ), $margin, $qty, $mat );
		} elseif ( $kind === 'postpress' ) {
			PM_Calcs::add_postpress( $cid, self::size_cm( $p, 'width' ), self::size_cm( $p, 'length' ), $qty, $mat );
		} elseif ( $kind === 'printing' ) {
			PM_Calcs::add_printing( $cid, self::val( $p, 'papier' ), self::val( $p, 'colour' ), $qty );
		} else {
			throw new PM_Error( 'Неизвестный вид расчёта' );
		}
		return PM_View::url( 'pm-new-order', array( 'customer' => $cid ) );
	}

	private static function do_calc_delete( array $p ) {
		$cid = (int) self::val( $p, 'customer_id' );
		PM_Calcs::delete( self::val( $p, 'kind' ), $cid, (int) self::val( $p, 'calc_id' ) );
		return PM_View::url( 'pm-new-order', array( 'customer' => $cid ) );
	}

	private static function int_list( array $p, $key ) {
		return isset( $p[ $key ] ) && is_array( $p[ $key ] ) ? array_map( 'intval', $p[ $key ] ) : array();
	}

	private static function do_order_create( array $p ) {
		$cid = (int) self::val( $p, 'customer_id' );
		// ручной расход материалов: поля m_<id материала>
		$consumption = array();
		foreach ( $p as $key => $raw ) {
			if ( strpos( $key, 'm_' ) === 0 && ctype_digit( substr( $key, 2 ) ) && is_string( $raw ) && trim( $raw ) !== '' ) {
				$q = PM_Num::parse( $raw );
				if ( $q === null || $q < 0 ) {
					throw new PM_Error( 'Количество материала указано неверно' );
				}
				$consumption[ (int) substr( $key, 2 ) ] = $q;
			}
		}
		$id = PM_Orders::create(
			$cid,
			self::val( $p, 'product' ),
			self::int_list( $p, 'postpress_ids' ),
			self::int_list( $p, 'printing_ids' ),
			self::int_list( $p, 'cliche_ids' ),
			$consumption
		);
		return array( 'ok' => 'Заказ №' . $id . ' оформлен', 'url' => PM_View::url( 'pm-orders', array( 'order' => $id, 'done' => 1 ) ) );
	}

	private static function do_order_status( array $p ) {
		$id = (int) self::val( $p, 'id' );
		PM_Orders::set_status( $id, self::val( $p, 'status' ) );
		if ( self::val( $p, 'back' ) === 'all' ) {
			$args = array( 'view' => 'all' );
			if ( trim( self::val( $p, 'q' ) ) !== '' ) {
				$args['q'] = trim( self::val( $p, 'q' ) );
			}
			return array( 'ok' => 'Статус заказа №' . $id . ' изменён', 'url' => PM_View::url( 'pm-orders', $args ) );
		}
		if ( self::val( $p, 'back' ) === 'order' ) {
			return array( 'ok' => 'Статус заказа изменён', 'url' => PM_View::url( 'pm-orders', array( 'order' => $id ) ) );
		}
		$order = PM_Orders::get( $id );
		return array( 'ok' => 'Статус заказа №' . $id . ' изменён', 'url' => PM_View::url( 'pm-orders', array( 'customer' => $order->customer_id ) ) );
	}

	private static function do_order_delete( array $p ) {
		$id    = (int) self::val( $p, 'id' );
		$order = PM_Orders::get( $id );
		PM_Orders::delete( $id );
		return array( 'ok' => 'Заказ удалён', 'url' => PM_View::url( 'pm-orders', array( 'customer' => $order->customer_id ) ) );
	}

	private static function do_material_create( array $p ) {
		$cat  = self::val( $p, 'category' );
		$note = self::val( $p, 'note' );
		if ( ! isset( PM_Units::CATEGORIES[ $cat ] ) ) {
			throw new PM_Error( 'Выберите раздел склада' );
		}
		if ( $cat === 'PAPER' ) {
			PM_Materials::create_paper( self::val( $p, 'name' ), self::val( $p, 'sheet_size' ), PM_Num::parse( self::val( $p, 'stock' ) ), $note );
		} elseif ( $cat === 'FOIL' ) {
			PM_Materials::create_foil(
				self::val( $p, 'foil_type' ),
				self::size_cm( $p, 'width' ),
				PM_Num::parse( self::val( $p, 'winding' ) ),
				PM_Num::parse( self::val( $p, 'rolls' ) ),
				$note
			);
		} else {
			PM_Materials::create_plate(
				self::val( $p, 'name' ),
				$cat,
				self::size_cm( $p, 'width' ),
				self::size_cm( $p, 'length' ),
				PM_Num::parse( self::val( $p, 'sheets' ) ),
				$note
			);
		}
		return array( 'ok' => 'Материал добавлен', 'url' => PM_View::url( 'pm-materials' ) );
	}

	private static function do_material_update( array $p ) {
		$id = (int) self::val( $p, 'id' );
		PM_Materials::update( $id, self::val( $p, 'name' ), self::val( $p, 'sheet_size' ), self::val( $p, 'note' ) );
		$w = self::size_cm( $p, 'width' );
		$winding = PM_Num::parse( self::val( $p, 'winding' ) );
		$l = self::size_cm( $p, 'length' );
		if ( $w !== null && $winding !== null ) {
			PM_Materials::update_size( $id, $w, PM_Num::round( $winding * 100, 3 ) ); // фольга: намотка в метрах
		} elseif ( $w !== null && $l !== null ) {
			PM_Materials::update_size( $id, $w, $l );
		}
		return array( 'ok' => 'Материал сохранён', 'url' => PM_View::url( 'pm-materials', array( 'id' => $id ) ) );
	}

	private static function do_material_receive( array $p ) {
		$id = (int) self::val( $p, 'id' );
		PM_Materials::receive( $id, PM_Num::parse( self::val( $p, 'quantity' ) ), self::val( $p, 'note' ) );
		return array( 'ok' => 'Приход записан', 'url' => PM_View::url( 'pm-materials', array( 'id' => $id ) ) );
	}

	private static function do_material_receive_sheets( array $p ) {
		$id = (int) self::val( $p, 'id' );
		PM_Materials::receive_sheets( $id, PM_Num::parse( self::val( $p, 'sheets' ) ), self::val( $p, 'note' ) );
		return array( 'ok' => 'Приход записан', 'url' => PM_View::url( 'pm-materials', array( 'id' => $id ) ) );
	}

	private static function do_material_consume( array $p ) {
		$id = (int) self::val( $p, 'id' );
		PM_Materials::consume( $id, PM_Num::parse( self::val( $p, 'quantity' ) ), self::val( $p, 'note' ) );
		return array( 'ok' => 'Расход записан', 'url' => PM_View::url( 'pm-materials', array( 'id' => $id ) ) );
	}

	private static function do_material_delete( array $p ) {
		PM_Materials::delete( (int) self::val( $p, 'id' ) );
		return array( 'ok' => 'Материал удалён', 'url' => PM_View::url( 'pm-materials' ) );
	}

	private static function do_settings_save( array $p ) {
		PM_Settings::save( $p );
		return array( 'ok' => 'Настройки сохранены', 'url' => PM_View::url( 'pm-settings' ) );
	}

	// ---------- печатная карточка ----------

	public static function print_card() {
		if ( ! current_user_can( PM_Db::CAP ) ) {
			wp_die( esc_html__( 'Недостаточно прав.', 'printmaster-crm' ), 403 );
		}
		$id = isset( $_GET['id'] ) ? (int) $_GET['id'] : 0; // phpcs:ignore WordPress.Security.NonceVerification
		try {
			$order    = PM_Orders::get( $id );
			$customer = PM_Customers::get( $order->customer_id );
			$items    = PM_Orders::materials_by_order( array( $order ) );
			$items    = isset( $items[ (int) $order->id ] ) ? $items[ (int) $order->id ] : array();
		} catch ( PM_Error $e ) {
			wp_die( esc_html( $e->getMessage() ), 404 );
		}
		nocache_headers();
		header( 'Content-Type: text/html; charset=utf-8' );
		include PM_DIR . 'views/print.php';
		exit;
	}
}

require_once PM_DIR . 'includes/class-pm-view.php';
