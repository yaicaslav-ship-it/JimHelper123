package ru.example.badoptimizations.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow public HitResult crosshairTarget;
    @Shadow public ClientPlayerEntity player;

    private HitResult badoptimizations$saved;
    private boolean badoptimizations$swapped;

    // Правый клик (использование предмета): если в руке паутина и прицел на игроке,
    // временно берём прицел "сквозь" игрока - на блок за ним.
    @Inject(method = "doItemUse()V", at = @At("HEAD"))
    private void badoptimizations$before(CallbackInfo ci) {
        badoptimizations$swapped = false;

        if (player == null || crosshairTarget == null) return;
        if (!(crosshairTarget instanceof EntityHitResult ehr)) return;
        if (!(ehr.getEntity() instanceof PlayerEntity)) return;

        boolean holdingWeb = player.getMainHandStack().isOf(Items.COBWEB)
                          || player.getOffHandStack().isOf(Items.COBWEB);
        if (!holdingWeb) return;

        // raycast без сущностей - попадает только в блоки
        HitResult blockHit = player.raycast(player.getBlockInteractionRange(), 1.0F, false);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            badoptimizations$saved = crosshairTarget;
            crosshairTarget = blockHit;
            badoptimizations$swapped = true;
        }
    }

    // После использования возвращаем прежний прицел, чтобы удар (ЛКМ) по игроку работал как обычно.
    @Inject(method = "doItemUse()V", at = @At("RETURN"))
    private void badoptimizations$after(CallbackInfo ci) {
        if (badoptimizations$swapped) {
            crosshairTarget = badoptimizations$saved;
            badoptimizations$swapped = false;
            badoptimizations$saved = null;
        }
    }
}
