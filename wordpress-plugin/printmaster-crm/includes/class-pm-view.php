<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Вывод страниц: общий каркас (меню, сообщения), ссылки, формы. */
class PM_View {

	/** Ссылка на страницу плагина: url( 'pm-orders', array( 'order' => 5 ) ). */
	public static function url( $page, array $args = array() ) {
		return add_query_arg( array_merge( array( 'page' => $page ), $args ), admin_url( 'admin.php' ) );
	}

	/** Начало POST-формы: действие, nonce и скрытые поля. */
	public static function form_open( $action, array $hidden = array(), $attrs = '' ) {
		$out  = '<form method="post" action="' . esc_url( admin_url( 'admin-post.php' ) ) . '" ' . $attrs . '>';
		$out .= '<input type="hidden" name="action" value="pm_' . esc_attr( $action ) . '">';
		$out .= wp_nonce_field( 'pm_action', 'pm_nonce', true, false );
		foreach ( $hidden as $k => $v ) {
			$out .= '<input type="hidden" name="' . esc_attr( $k ) . '" value="' . esc_attr( $v ) . '">';
		}
		return $out;
	}

	public static function render( $view, $page ) {
		$settings = PM_Settings::get();
		$lu       = $settings['length_unit'];
		$ul       = PM_Units::length_label( $lu );
		self::open( $page );
		include PM_DIR . 'views/' . $view . '.php';
		self::close();
	}

	public static function render_error( $message, $page ) {
		self::open( $page );
		echo '<main><div class="alert error">' . esc_html( $message ) . '</div><p><a href="' . esc_url( self::url( 'pm' ) ) . '">← На главную</a></p></main>';
		self::close();
	}

	private static function open( $page ) {
		$items = array(
			'pm-customers' => 'Клиенты',
			'pm-orders'    => 'Заказы',
			'pm-materials' => 'Материалы',
			'pm-settings'  => 'Настройки',
		);
		echo '<div class="pm-app">';
		echo '<nav class="top"><a class="brand" href="' . esc_url( self::url( 'pm' ) ) . '">PrintMaster</a>';
		foreach ( $items as $slug => $label ) {
			$active = ( $slug === $page || ( $slug === 'pm-orders' && $page === 'pm-new-order' ) ) ? ' active' : '';
			echo '<a class="' . esc_attr( trim( $active ) ) . '" href="' . esc_url( self::url( $slug ) ) . '">' . esc_html( $label ) . '</a>';
		}
		echo '<a class="quit" href="' . esc_url( admin_url() ) . '">← Админка сайта</a></nav>';
		$flash = PM_Admin::take_flash();
		if ( $flash ) {
			$cls = $flash['type'] === 'error' ? 'error' : 'ok';
			echo '<div class="flash"><div class="alert ' . esc_attr( $cls ) . '" role="status">' . esc_html( $flash['msg'] ) . '</div></div>';
		}
	}

	private static function close() {
		echo '</div>';
	}
}

/** Короткие помощники для шаблонов. */
function pm_h( $s ) {
	return esc_html( (string) $s );
}

function pm_a( $s ) {
	return esc_attr( (string) $s );
}

function pm_date( $mysql, $with_time = false ) {
	if ( ! $mysql ) {
		return '';
	}
	$ts = strtotime( $mysql );
	return $ts ? gmdate( $with_time ? 'd.m.Y H:i' : 'd.m.Y', $ts ) : '';
}

function pm_status_badge( $status ) {
	return '<span class="badge st-' . esc_attr( $status ) . '">' . esc_html( PM_Units::status_label( $status ) ) . '</span>';
}
