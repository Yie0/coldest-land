import net.minecraft.network.chat.Component
import net.minecraft.util.Util

data class Alert(
    val component: Component,
    val scale: Float,
    val lifetime: Int,
    val creationTime: Long = Util.getMillis(),
){
}