<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Справочники: единицы, разделы склада, типы фольги, статусы. */
class PM_Units {

	const LENGTH = array(
		'CM' => array( 'label' => 'см', 'cm' => 1.0 ),
		'MM' => array( 'label' => 'мм', 'cm' => 0.1 ),
	);

	const AREA = array(
		'CM2' => array( 'label' => 'см²', 'cm2' => 1.0 ),
		'M2'  => array( 'label' => 'м²', 'cm2' => 10000.0 ),
	);

	/** Разделы склада: единица учёта определяется разделом. */
	const CATEGORIES = array(
		'PAPER'     => array( 'label' => 'Бумага и картон', 'unit' => 'SHEET' ),
		'MAGNESIUM' => array( 'label' => 'Клише: магний', 'unit' => 'CM2' ),
		'BRASS'     => array( 'label' => 'Клише: латунь', 'unit' => 'CM2' ),
		'FOIL'      => array( 'label' => 'Фольга для тиснения', 'unit' => 'CM2' ),
	);

	const STOCK_UNITS = array(
		'SHEET' => array( 'label' => 'лист', 'whole' => true ),
		'CM2'   => array( 'label' => 'см²', 'whole' => false ),
	);

	const FOIL_TYPES = array(
		'GOLD'          => 'Золото',
		'SILVER'        => 'Серебро',
		'MATTE_GOLD'    => 'Матовое золото',
		'MATTE_SILVER'  => 'Матовое серебро',
		'COLOR'         => 'Цветная фольга',
	);

	const STATUSES = array(
		'NEW'         => 'Новый',
		'IN_PROGRESS' => 'В работе',
		'READY'       => 'Готов',
		'DONE'        => 'Выполнен',
	);

	const PAPERS  = array( '90g/m2', '150g/m2', '200g/m2', '300g/m2' );
	const COLOURS = array( '4+0', '4+4' );

	public static function length_label( $unit ) {
		return self::LENGTH[ $unit ]['label'];
	}

	/** Введённый размер в выбранных единицах -> сантиметры (3 знака). */
	public static function to_cm( $value, $unit ) {
		return PM_Num::round( $value * self::LENGTH[ $unit ]['cm'], 3 );
	}

	/** Размер из базы (см) в выбранных единицах без лишних нулей. */
	public static function fmt_len( $cm, $unit ) {
		if ( $cm === null || $cm === '' ) {
			return '';
		}
		return PM_Num::plain( (float) $cm / self::LENGTH[ $unit ]['cm'], 3 );
	}

	public static function cat_label( $category ) {
		return isset( self::CATEGORIES[ $category ] ) ? self::CATEGORIES[ $category ]['label'] : $category;
	}

	public static function cat_unit( $category ) {
		return self::STOCK_UNITS[ self::CATEGORIES[ $category ]['unit'] ];
	}

	public static function status_label( $status ) {
		return isset( self::STATUSES[ $status ] ) ? self::STATUSES[ $status ] : $status;
	}
}
