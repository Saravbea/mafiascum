<?php
/**
*
* acp_users [English]
*
* @package language
* @version $Id$
* @copyright (c) 2005 phpBB Group
* @license http://opensource.org/licenses/gpl-license.php GNU Public License
*
*/

/**
* DO NOT CHANGE
*/
if (!defined('IN_PHPBB'))
{
	exit;
}

if (empty($lang) || !is_array($lang))
{
	$lang = array();
}

// DEVELOPERS PLEASE NOTE
//
// All language files should use UTF-8 as their encoding and the files must not contain a BOM.
//
// Placeholders can now contain order information, e.g. instead of
// 'Page %s of %s' you can (and should) write 'Page %1$s of %2$s', this allows
// translators to re-order the output of data while ensuring it remains correct
//
// You do not need this where single placeholders are used, e.g. 'Message %d' is fine
// equally where a string contains only two placeholders which are used to wrap text
// in a url you again do not need to specify an order e.g., 'Click %sHERE%s' is fine

$lang = array_merge($lang, array(
	'SELECT_USER'					=> 'Select user',
	'ACP_USER_RANK'				    => 'change title',
	'TITLE_ADMIN'					=> 'Title administration',
	'TITLE_ADMIN_EXPLAIN'			=> 'Here you can change a users title.',
	'USER_PROFILE_UPDATED'			=> 'User profile updated.',
	'USER_RANK'						=> 'User rank',
	'USER_RANK_UPDATED'				=> 'User rank updated.'
));

?>