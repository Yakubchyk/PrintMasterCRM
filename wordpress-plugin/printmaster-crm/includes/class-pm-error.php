<?php
if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/** Ошибка, текст которой можно показать пользователю (нарушено бизнес-правило, не найдена запись и т. п.). */
class PM_Error extends Exception {
}
