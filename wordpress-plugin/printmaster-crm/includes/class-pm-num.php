<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Числа: разбор ввода, округление и вывод. Деньги и размеры — float с округлением до нужного знака. */
class PM_Num {

	/** Разбирает число из формы (допускает запятую и пробелы). null — если пусто или не число. */
	public static function parse( $raw ) {
		if ( is_int( $raw ) || is_float( $raw ) ) {
			return (float) $raw;
		}
		if ( ! is_string( $raw ) ) {
			return null;
		}
		$s = str_replace( array( ' ', "\xC2\xA0" ), '', trim( $raw ) );
		$s = str_replace( ',', '.', $s );
		if ( $s === '' || ! is_numeric( $s ) ) {
			return null;
		}
		return (float) $s;
	}

	/** Целое число из формы; null — если пусто, не число или дробное. */
	public static function parse_int( $raw ) {
		$v = self::parse( $raw );
		if ( $v === null || floor( $v ) != $v ) {
			return null;
		}
		return (int) $v;
	}

	/** Округление «half up» до $places знаков (для неотрицательных чисел). */
	public static function round( $value, $places = 2 ) {
		return round( (float) $value + 0.0, $places, PHP_ROUND_HALF_UP );
	}

	/** Число без лишних нулей: 12.5, 100, 756400. */
	public static function plain( $value, $places = 3 ) {
		$s = number_format( (float) $value, $places, '.', '' );
		if ( strpos( $s, '.' ) !== false ) {
			$s = rtrim( rtrim( $s, '0' ), '.' );
		}
		return ( $s === '' || $s === '-0' ) ? '0' : $s;
	}

	/** Деньги для показа: «1 234,50». */
	public static function money( $value ) {
		return number_format( (float) $value, 2, ',', "\xC2\xA0" );
	}

	/** Количество для показа с пробелами между тысячами: «756 400», «12,5». */
	public static function qty( $value, $places = 3 ) {
		$plain = self::plain( $value, $places );
		$parts = explode( '.', $plain );
		$int   = number_format( (float) $parts[0], 0, '', "\xC2\xA0" );
		return isset( $parts[1] ) ? $int . ',' . $parts[1] : $int;
	}
}
