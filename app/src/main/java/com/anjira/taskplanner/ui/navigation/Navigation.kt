package com.anjira.taskplanner.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val GROUP_LIST = "group_list"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    fun groupDetail(groupId: Int) = "group_detail/$groupId"
}
