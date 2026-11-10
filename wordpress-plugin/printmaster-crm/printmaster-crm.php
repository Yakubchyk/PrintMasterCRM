<?php
/**
 * Plugin Name:       PrintMaster CRM
 * Description:       Клиенты, заказы, расчёты (клише, тиснение, печать) и склад материалов для печатной мастерской. Работает в админке WordPress, доступно только администратору.
 * Version:           1.0.0
 * Requires at least: 6.0
 * Requires PHP:      7.4
 * Author:            Ivan Yakubchyk
 * Text Domain:       printmaster-crm
 * License:           Proprietary
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

define( 'PM_VERSION', '1.0.0' );
define( 'PM_FILE', __FILE__ );
define( 'PM_DIR', plugin_dir_path( __FILE__ ) );
define( 'PM_URL', plugin_dir_url( __FILE__ ) );

require_once PM_DIR . 'includes/class-pm-error.php';
require_once PM_DIR . 'includes/class-pm-num.php';
require_once PM_DIR . 'includes/class-pm-units.php';
require_once PM_DIR . 'includes/class-pm-db.php';
require_once PM_DIR . 'includes/class-pm-settings.php';
require_once PM_DIR . 'includes/class-pm-calc.php';
require_once PM_DIR . 'includes/class-pm-customers.php';
require_once PM_DIR . 'includes/class-pm-materials.php';
require_once PM_DIR . 'includes/class-pm-calcs.php';
require_once PM_DIR . 'includes/class-pm-orders.php';
require_once PM_DIR . 'includes/class-pm-admin.php';

register_activation_hook( __FILE__, array( 'PM_Db', 'activate' ) );

add_action( 'plugins_loaded', array( 'PM_Db', 'maybe_upgrade' ) );

if ( is_admin() ) {
	PM_Admin::init();
}
