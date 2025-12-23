import net.minecraft.network.chat.Component

data class Alert(
    val component: Component,
    val lifetime: Int,
    val creationTime: Long,
){
}