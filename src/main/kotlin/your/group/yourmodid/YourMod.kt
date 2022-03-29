package your.group.yourmodid

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager
import thedarkcolour.kotlinforforge.forge.runForDist

// for some reason when this object is being compiled to a java class and kotlinforforge is not recognizing it correctly, so im gonna make this a class
@Mod(modId)
class YourMod {
    init {
//        LOADING_CONTEXT.registerExtensionPoint(
//            DisplayTest::class.java
//        ) {
//            DisplayTest(
//                { NetworkConstants.IGNORESERVERONLY }
//            ) { _, _ -> true }
//        }

        logger.info("Hello world, from $displayName!")
        runForDist({
            logger.info("On client")
            // run client only code
        }, {
            logger.info("On server")
            // run server only code
        })
    }
}

// create location with custom path, and with namespace of your mod id
internal fun loc(id: String) = ResourceLocation(modId, id)

// get minecraft client, will crash on server!
internal inline val minecraft get() = Minecraft.getInstance()

// get your mod's logger
internal inline val logger get() = LogManager.getLogger(modId)
