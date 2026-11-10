<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Настройки цен и единиц (одна запись в wp_options). */
class PM_Settings {

	const OPTION = 'pm_settings';

	public static function defaults() {
		return array(
			'length_unit'      => 'CM',
			'montage_price'    => 0.0,   // приладка тиснения
			'one_ottisk_price' => 0.0,   // цена одного оттиска
			'foil_price'       => 0.0,   // цена фольги за единицу площади
			'foil_area_unit'   => 'M2',
			'print_price'      => 0.0,   // лист, одна сторона, бумага 90 г
			'cliche_price'     => 0.0,   // цена клише за единицу площади
			'cliche_area_unit' => 'CM2',
			'cliche_min_price' => 0.0,
			'markup'           => 1.25,  // только для тиснения
		);
	}

	public static function get() {
		$saved = get_option( self::OPTION, array() );
		return array_merge( self::defaults(), is_array( $saved ) ? $saved : array() );
	}

	/** Сохраняет настройки из формы (строки), проверяя значения. */
	public static function save( array $form ) {
		$out    = self::defaults();
		$fields = array(
			'montage_price'    => 'Приладка',
			'one_ottisk_price' => 'Цена оттиска',
			'foil_price'       => 'Цена фольги',
			'print_price'      => 'Цена печати',
			'cliche_price'     => 'Цена клише',
			'cliche_min_price' => 'Минимальная цена клише',
		);
		foreach ( $fields as $key => $label ) {
			$v = PM_Num::parse( isset( $form[ $key ] ) ? $form[ $key ] : '' );
			if ( $v === null || $v < 0 ) {
				throw new PM_Error( $label . ': укажите число не меньше нуля' );
			}
			$out[ $key ] = $v;
		}
		$markup = PM_Num::parse( isset( $form['markup'] ) ? $form['markup'] : '' );
		if ( $markup === null || $markup <= 0 ) {
			throw new PM_Error( 'Коэффициент должен быть больше нуля' );
		}
		$out['markup'] = $markup;

		$lu = isset( $form['length_unit'] ) ? $form['length_unit'] : 'CM';
		$out['length_unit'] = isset( PM_Units::LENGTH[ $lu ] ) ? $lu : 'CM';
		foreach ( array( 'foil_area_unit', 'cliche_area_unit' ) as $key ) {
			$au          = isset( $form[ $key ] ) ? $form[ $key ] : '';
			$out[ $key ] = isset( PM_Units::AREA[ $au ] ) ? $au : self::defaults()[ $key ];
		}
		update_option( self::OPTION, $out, false );
		return $out;
	}
}
