<?php
/**
 * Проверка логики плагина на тестовом WordPress (запускать только на тестовой базе — таблицы очищаются!).
 *   WP_LOAD=/путь/к/wp-load.php php tests/run.php
 */
if ( PHP_SAPI !== 'cli' ) {
	exit( 'Только из командной строки' );
}
$_SERVER['HTTP_HOST']   = $_SERVER['HTTP_HOST'] ?? 'localhost';
$_SERVER['REQUEST_URI'] = '/';
require getenv( 'WP_LOAD' ) ?: __DIR__ . '/../../../../wp-load.php';

global $wpdb;
$pass = 0;
$fail = 0;
function t( $name, $cond, $info = '' ) {
	global $pass, $fail;
	if ( $cond ) {
		$pass++;
	} else {
		$fail++;
		echo "FAIL: $name $info\n";
	}
}
function eq( $name, $actual, $expected, $eps = 0.0001 ) {
	t( $name, abs( (float) $actual - (float) $expected ) < $eps, "(получено $actual, ожидалось $expected)" );
}
function throws( $name, $fn, $contains = '' ) {
	try {
		$fn();
		t( $name, false, '(исключения не было)' );
	} catch ( PM_Error $e ) {
		t( $name, $contains === '' || mb_strpos( $e->getMessage(), $contains ) !== false, '(сообщение: ' . $e->getMessage() . ')' );
	}
}

// чистое состояние
foreach ( array( 'order_materials', 'orders', 'cliche', 'printing', 'postpress', 'movements', 'materials', 'customers' ) as $tb ) {
	$wpdb->query( 'DELETE FROM ' . PM_Db::t( $tb ) );
}
delete_option( PM_Settings::OPTION );

// ---------- формулы (значения из тестов Java-версии) ----------
$std = array_merge( PM_Settings::defaults(), array(
	'montage_price' => 100, 'one_ottisk_price' => 2, 'foil_price' => 50, 'foil_area_unit' => 'M2',
	'print_price' => 0.10, 'cliche_price' => 0.5, 'cliche_area_unit' => 'CM2', 'cliche_min_price' => 20, 'markup' => 1.25,
) );
eq( 'тиснение 10x10 тираж 10', PM_Calc::postpress( 10, 10, 10, $std ), 164.06 );
eq( 'тиснение 5x5 -> мин. 50 см²', PM_Calc::postpress( 5, 5, 10, $std ), 160.78 );
$s2 = $std; $s2['foil_price'] = 0.005; $s2['foil_area_unit'] = 'CM2';
eq( 'фольга за см² = за м²', PM_Calc::postpress( 10, 10, 10, $s2 ), 164.06 );
$s3 = $std; $s3['markup'] = 2;
eq( 'коэффициент из настроек', PM_Calc::postpress( 10, 10, 10, $s3 ), 262.50 );
eq( 'печать 90 4+0 1000', PM_Calc::printing( '90g/m2', '4+0', 1000, $std ), 100.00 );
eq( 'печать 150 4+4 1000', PM_Calc::printing( '150g/m2', '4+4', 1000, $std ), 240.00 );
eq( 'печать 200', PM_Calc::printing( '200g/m2', '4+0', 1000, $std ), 140.00 );
eq( 'печать 300', PM_Calc::printing( '300g/m2', '4+0', 1000, $std ), 160.00 );
eq( 'печать неизвестная бумага', PM_Calc::printing( '???', '4+0', 1000, $std ), 100.00 );
eq( 'клише 4x5 x3 -> минимум 20 × 3', PM_Calc::cliche( 4, 5, 3, $std ), 60.00 );
eq( 'клише 10x10 x2', PM_Calc::cliche( 10, 10, 2, $std ), 100.00 );
$s4 = $std; $s4['cliche_price'] = 5000; $s4['cliche_area_unit'] = 'M2';
eq( 'клише за м² = за см²', PM_Calc::cliche( 10, 10, 2, $s4 ), 100.00 );
eq( 'поля: 10 + 2*1', PM_Calc::with_margins( 10, 1 ), 12 );

// ---------- настройки ----------
PM_Settings::save( array(
	'length_unit' => 'CM', 'montage_price' => '100', 'one_ottisk_price' => '2', 'foil_price' => '50', 'foil_area_unit' => 'M2',
	'print_price' => '0,10', 'cliche_price' => '0.5', 'cliche_area_unit' => 'CM2', 'cliche_min_price' => '20', 'markup' => '1.25',
) );
$s = PM_Settings::get();
eq( 'настройки: запятая как разделитель', $s['print_price'], 0.10 );
throws( 'настройки: отрицательная цена', function () { PM_Settings::save( array( 'montage_price' => '-1' ) ); } );
throws( 'настройки: коэффициент 0', function () {
	PM_Settings::save( array( 'montage_price' => '1', 'one_ottisk_price' => '1', 'foil_price' => '1', 'print_price' => '1', 'cliche_price' => '1', 'cliche_min_price' => '1', 'markup' => '0' ) );
}, 'Коэффициент' );

// ---------- единицы ----------
eq( 'мм -> см', PM_Units::to_cm( 150, 'MM' ), 15 );
t( 'формат размера', PM_Units::fmt_len( 15, 'MM' ) === '150' && PM_Units::fmt_len( 12.5, 'CM' ) === '12.5' );

// ---------- клиенты ----------
$a = PM_Customers::create( array( 'name' => 'Альфа Принт', 'phone' => '+49 111', 'email' => 'a@example.com', 'postal_code' => '24787', 'city' => 'Fockbek', 'street' => 'Hauptstr.', 'house_number' => '5' ) );
$b = PM_Customers::create( array( 'name' => 'Бета', 'phone' => '+49 222', 'email' => 'b@example.com' ) );
t( 'адрес одной строкой', PM_Customers::address_line( PM_Customers::get( $a ) ) === 'Hauptstr. 5, 24787 Fockbek' );
t( 'адрес пустой', PM_Customers::address_line( PM_Customers::get( $b ) ) === '' );
throws( 'клиент: дубль названия', function () { PM_Customers::create( array( 'name' => 'альфа принт', 'phone' => '1', 'email' => 'x@example.com' ) ); }, 'названием' );
throws( 'клиент: дубль телефона', function () { PM_Customers::create( array( 'name' => 'X', 'phone' => '+49 111', 'email' => 'x@example.com' ) ); }, 'телефоном' );
throws( 'клиент: дубль email', function () { PM_Customers::create( array( 'name' => 'X', 'phone' => '7', 'email' => 'A@example.com' ) ); }, 'email' );
throws( 'клиент: пустое название', function () { PM_Customers::create( array( 'name' => ' ', 'phone' => '7', 'email' => 'x@example.com' ) ); }, 'Заполните' );
throws( 'клиент: неверный email', function () { PM_Customers::create( array( 'name' => 'Y', 'phone' => '8', 'email' => 'не-почта' ) ); }, 'email' );
PM_Customers::update( $b, array( 'name' => 'Бета Дизайн', 'phone' => '+49 222', 'email' => 'b@example.com' ) );
t( 'клиент: обновление своих данных без ложного дубля', PM_Customers::get( $b )->name === 'Бета Дизайн' );
t( 'поиск клиента по части названия', count( PM_Customers::all( 'принт' ) ) === 1 );
t( 'поиск клиента по телефону', count( PM_Customers::all( '222' ) ) === 1 );
t( 'поиск: спецсимвол % не ломает запрос', count( PM_Customers::all( '%' ) ) === 0 );

// ---------- склад ----------
$mg = PM_Materials::create_plate( 'Магний 3 мм', 'MAGNESIUM', 100, 50, 10, 'заметка' ); // 100×50 = 5000 см² × 10 = 50000
$m  = PM_Materials::get( $mg );
eq( 'магний: остаток в см²', $m->stock, 50000 );
$paper = PM_Materials::create_paper( 'Мелованная 300', '70x100', 500.0, null );
eq( 'бумага: остаток в листах', PM_Materials::get( $paper )->stock, 500 );
$foil = PM_Materials::create_foil( 'GOLD', 62, 122, 2, null ); // 62 × 12200 × 2 = 1 512 800
$fm   = PM_Materials::get( $foil );
t( 'фольга: название', $fm->name === 'Золото 62 см × 122 м', $fm->name );
eq( 'фольга: остаток', $fm->stock, 1512800 );
eq( 'фольга: намотка в метрах', PM_Materials::roll_length_m( $fm ), 122 );
throws( 'фольга: дубль', function () { PM_Materials::create_foil( 'GOLD', 62, 122, 1, null ); }, 'уже есть' );
throws( 'бумага: дробные листы', function () { PM_Materials::create_paper( 'Другая', '', 1.5, null ); }, 'целое' );
PM_Materials::receive_sheets( $mg, 2, 'поставка' );
eq( 'приход листами', PM_Materials::get( $mg )->stock, 60000 );
PM_Materials::consume( $mg, 1000, 'тест' );
eq( 'расход', PM_Materials::get( $mg )->stock, 59000 );
throws( 'расход больше остатка', function () use ( $mg ) { PM_Materials::consume( $mg, 1000000, '' ); }, 'недостаточно' );
throws( 'бумага: дробный расход', function () use ( $paper ) { PM_Materials::consume( $paper, 1.5, '' ); }, 'целым' );
t( 'история движения', count( PM_Materials::movements( $mg ) ) === 3 );

// ---------- расчёты и заказ ----------
$c1 = PM_Calcs::add_cliche( $a, 10, 10, 1, 2, $mg );        // с полями 12×12 = 144, ×2 = 288 см²; цена 144*0.5=72 ×2 = 144
$cl = PM_Db::row( 'SELECT * FROM ' . PM_Db::t( 'cliche' ) . ' WHERE id = %d', array( $c1 ) );
eq( 'клише: цена по общему размеру', $cl->total_price, 144.00 );
eq( 'клише: общая площадь', PM_Calcs::cliche_area( $cl ), 144 );
eq( 'клише: общая площадь × количество', PM_Calcs::cliche_total_area( $cl ), 288 );
$p1 = PM_Calcs::add_postpress( $a, 10, 10, 10, $foil );     // фольга 10×10×10 = 1000 см²; цена 164.06
$pr1 = PM_Calcs::add_printing( $a, '150g/m2', '4+4', 1000 ); // 240.00
throws( 'клише: нельзя фольгу', function () use ( $a, $foil ) { PM_Calcs::add_cliche( $a, 1, 1, 0, 1, $foil ); }, 'магний или латунь' );
throws( 'тиснение: нельзя магний', function () use ( $a, $mg ) { PM_Calcs::add_postpress( $a, 1, 1, 1, $mg ); }, 'Фольга' );

throws( 'заказ: без расчётов', function () use ( $a ) { PM_Orders::create( $a, 'Описание', array(), array(), array(), array() ); }, 'хотя бы один' );
throws( 'заказ: без описания', function () use ( $a, $c1 ) { PM_Orders::create( $a, ' ', array(), array(), array( $c1 ), array() ); }, 'детали' );
throws( 'заказ: чужой расчёт', function () use ( $b, $c1 ) { PM_Orders::create( $b, 'Чужой', array(), array(), array( $c1 ), array() ); }, 'не найден' );

$stock_before = (float) PM_Materials::get( $mg )->stock;
$oid = PM_Orders::create( $a, 'Визитки 100 шт', array( $p1 ), array( $pr1 ), array( $c1 ), array( $paper => 40.0 ) );
$o   = PM_Orders::get( $oid );
eq( 'заказ: итог = клише + тиснение + печать', $o->total_cost, 144.00 + 164.06 + 240.00 );
eq( 'заказ: цена клише', $o->cliche_price, 144.00 );
eq( 'заказ: цена тиснения', $o->postpress_price, 164.06 );
eq( 'заказ: цена печати', $o->printing_price, 240.00 );
t( 'заказ: статус Новый', $o->status === 'NEW' );
t( 'заказ: состав работ записан', mb_strpos( $o->details, 'Клише: 10 × 10 см, поля 1 см' ) !== false && mb_strpos( $o->details, 'Тиснение:' ) !== false && mb_strpos( $o->details, 'Печать: бумага 150g/m2, 4+4, тираж 1000 — 240.00' ) !== false, $o->details );
eq( 'резерв магния', PM_Materials::get( $mg )->reserved, 288 );
eq( 'резерв фольги', PM_Materials::get( $foil )->reserved, 1000 );
eq( 'резерв бумаги', PM_Materials::get( $paper )->reserved, 40 );
eq( 'остаток магния не изменился до выполнения', PM_Materials::get( $mg )->stock, $stock_before );
eq( 'свободно магния', PM_Materials::available( PM_Materials::get( $mg ) ), $stock_before - 288 );
throws( 'расход нельзя из резерва', function () use ( $paper ) { PM_Materials::consume( $paper, 480, '' ); }, 'недостаточно' );

// заказ с нехваткой материала откатывается целиком
$orders_before = (int) $wpdb->get_var( 'SELECT COUNT(*) FROM ' . PM_Db::t( 'orders' ) );
$res_before    = (float) PM_Materials::get( $paper )->reserved;
$c2 = PM_Calcs::add_cliche( $a, 10, 10, 0, 1, $mg );
throws( 'заказ: нехватка бумаги', function () use ( $a, $c2, $paper ) { PM_Orders::create( $a, 'Много', array(), array(), array( $c2 ), array( $paper => 10000.0 ) ); }, 'недостаточно' );
t( 'откат: заказ не создан', (int) $wpdb->get_var( 'SELECT COUNT(*) FROM ' . PM_Db::t( 'orders' ) ) === $orders_before );
eq( 'откат: резерв бумаги прежний', PM_Materials::get( $paper )->reserved, $res_before );
eq( 'откат: резерв магния прежний', PM_Materials::get( $mg )->reserved, 288 );

// статусы
PM_Orders::set_status( $oid, 'IN_PROGRESS' );
eq( 'в работе: остаток не тронут', PM_Materials::get( $mg )->stock, $stock_before );
PM_Orders::set_status( $oid, 'DONE' );
$mg1 = PM_Materials::get( $mg );
eq( 'выполнен: магний списан', $mg1->stock, $stock_before - 288 );
eq( 'выполнен: резерв магния снят', $mg1->reserved, 0 );
eq( 'выполнен: фольга списана', PM_Materials::get( $foil )->stock, 1512800 - 1000 );
eq( 'выполнен: бумага списана', PM_Materials::get( $paper )->stock, 460 );
t( 'выполнен: дата завершения', PM_Orders::get( $oid )->completed_at !== null );
PM_Orders::set_status( $oid, 'DONE' );
eq( 'повторный «Выполнен» не списывает дважды', PM_Materials::get( $mg )->stock, $stock_before - 288 );
PM_Orders::set_status( $oid, 'READY' );
$mg2 = PM_Materials::get( $mg );
eq( 'возврат из «Выполнен»: склад восстановлен', $mg2->stock, $stock_before );
eq( 'возврат из «Выполнен»: снова в резерве', $mg2->reserved, 288 );
t( 'возврат из «Выполнен»: дата сброшена', PM_Orders::get( $oid )->completed_at === null );
PM_Orders::set_status( $oid, 'DONE' );
throws( 'неизвестный статус', function () use ( $oid ) { PM_Orders::set_status( $oid, 'XXX' ); }, 'Неизвестный' );

// поиск и сводка
t( 'поиск по номеру', count( PM_Orders::search( (string) $oid ) ) === 1 );
t( 'поиск по описанию', count( PM_Orders::search( 'визитки' ) ) === 1 );
t( 'поиск по материалу', count( PM_Orders::search( 'мелованная' ) ) === 1 );
t( 'поиск по клиенту', count( PM_Orders::search( 'Альфа' ) ) === 1 );
t( 'поиск по составу работ', count( PM_Orders::search( 'Тиснение' ) ) === 1 );
t( 'поиск без совпадений', count( PM_Orders::search( 'ничего-такого' ) ) === 0 );
$sum = PM_Orders::summaries( PM_Customers::all() );
t( 'сводка: клиент с заказами первый', (int) $sum[0]->customer->id === (int) $a );
t( 'сводка: счётчики', $sum[0]->order_count === 1 && $sum[0]->active_count === 0 );

// второй заказ — удаление до выполнения снимает резерв
$oid2 = PM_Orders::create( $a, 'Второй', array(), array(), array( $c2 ), array() );
eq( 'второй заказ: резерв магния', PM_Materials::get( $mg )->reserved, 100 );
PM_Orders::delete( $oid2 );
eq( 'удаление: резерв снят', PM_Materials::get( $mg )->reserved, 0 );

// материал, использованный в заказах, удалить нельзя
throws( 'нельзя удалить материал из заказов', function () use ( $mg ) { PM_Materials::delete( $mg ); }, 'удалить его нельзя' );

// удаление выполненного заказа возвращает материалы на склад
$stock_done = (float) PM_Materials::get( $mg )->stock;
PM_Orders::delete( $oid );
eq( 'удаление выполненного: магний вернулся', PM_Materials::get( $mg )->stock, $stock_done + 288 );
eq( 'удаление выполненного: бумага вернулась', PM_Materials::get( $paper )->stock, 500 );

// удаление клиента вместе с заказами
$oid3 = PM_Orders::create( $a, 'Для удаления', array( $p1 ), array(), array(), array() );
PM_Customers::delete( $a );
t( 'клиент удалён вместе с расчётами и заказами', (int) $wpdb->get_var( 'SELECT COUNT(*) FROM ' . PM_Db::t( 'orders' ) . ' WHERE customer_id = ' . (int) $a ) === 0
	&& (int) $wpdb->get_var( 'SELECT COUNT(*) FROM ' . PM_Db::t( 'postpress' ) . ' WHERE customer_id = ' . (int) $a ) === 0 );
eq( 'удаление клиента: резерв фольги снят', PM_Materials::get( $foil )->reserved, 0 );

// удаление материала без заказов
$tmp = PM_Materials::create_paper( 'Лишний', '', 5.0, null );
PM_Materials::delete( $tmp );
throws( 'материал удалён', function () use ( $tmp ) { PM_Materials::get( $tmp ); }, 'не найден' );

echo "\nПройдено: $pass, провалено: $fail\n";
exit( $fail ? 1 : 0 );
