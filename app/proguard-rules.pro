# Glance 通过类名反射实例化点击动作，需保留无参构造喵
-keep class * extends androidx.glance.appwidget.action.ActionCallback {
    <init>();
}
