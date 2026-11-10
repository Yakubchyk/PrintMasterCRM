<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Чистые формулы расчётов (без базы данных). Размеры — в сантиметрах. */
class PM_Calc {

	/** Минимальная площадь оттиска, см²: всё, что меньше, считается как эта площадь. */
	const MIN_AREA_CM2 = 50.0;

	/** Запас на работу и фольгу. */
	const COEFFICIENT_WORK = 1.05;

	/**
	 * Тиснение: (работа + фольга) * коэффициент, где
	 * работа = (приладка + оттиск * тираж) * 1.05,
	 * фольга = тираж * площадь * 1.05 * цена фольги за единицу площади.
	 */
	public static function postpress( $width_cm, $length_cm, $quantity, array $s ) {
		$area = $width_cm * $length_cm;
		if ( $area <= self::MIN_AREA_CM2 ) {
			$area = self::MIN_AREA_CM2;
		}
		$foil = $quantity * $area * self::COEFFICIENT_WORK * $s['foil_price'] / PM_Units::AREA[ $s['foil_area_unit'] ]['cm2'];
		$work = ( $s['montage_price'] + $s['one_ottisk_price'] * $quantity ) * self::COEFFICIENT_WORK;
		return PM_Num::round( ( $work + $foil ) * $s['markup'], 2 );
	}

	/** Печать: цена печати * цветность * тираж * множитель плотности бумаги (коэффициент не применяется). */
	public static function printing( $papier, $colour, $quantity, array $s ) {
		$color_mult = ( $colour === '4+4' ) ? 2.0 : 1.0;
		return PM_Num::round( $s['print_price'] * $color_mult * $quantity * self::density( $papier ), 2 );
	}

	public static function density( $papier ) {
		switch ( $papier ) {
			case '150g/m2':
				return 1.2;
			case '200g/m2':
				return 1.4;
			case '300g/m2':
				return 1.6;
			default:
				return 1.0;
		}
	}

	/**
	 * Клише: размеры здесь общие (с полями). Одно клише = max(площадь * цена за единицу площади, минимальная цена).
	 * Итог = цена одного * количество (коэффициент не применяется).
	 */
	public static function cliche( $total_width_cm, $total_length_cm, $quantity, array $s ) {
		$area = $total_width_cm * $total_length_cm;
		$one  = PM_Num::round( $area * $s['cliche_price'] / PM_Units::AREA[ $s['cliche_area_unit'] ]['cm2'], 10 );
		if ( $one < $s['cliche_min_price'] ) {
			$one = $s['cliche_min_price'];
		}
		return PM_Num::round( $one * $quantity, 2 );
	}

	/** Общий размер клише с полями: поле добавляется с каждой стороны. */
	public static function with_margins( $size_cm, $margin_cm ) {
		return $size_cm + 2 * $margin_cm;
	}
}
