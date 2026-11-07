package pl.nicevisuals.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import pl.nicevisuals.setting.ModeSetting;
import pl.nicevisuals.setting.NumSetting;

public class HitSound extends Module {
    private final ModeSetting sound = add(new ModeSetting("Sound", 0, "Orb", "Bell", "Pling", "Arrow", "Level up", "Crit"));
    private final NumSetting pitch = add(new NumSetting("Pitch %", 100, 50, 200));
    private final NumSetting volume = add(new NumSetting("Volume %", 80, 10, 100));

    public HitSound() { super("Hit Sound", Category.COMBAT); }

    public void play() {
        SoundEvent ev = switch (sound.index) {
            case 1 -> SoundEvents.BLOCK_NOTE_BLOCK_BELL.value();
            case 2 -> SoundEvents.BLOCK_NOTE_BLOCK_PLING.value();
            case 3 -> SoundEvents.ENTITY_ARROW_HIT_PLAYER;
            case 4 -> SoundEvents.ENTITY_PLAYER_LEVELUP;
            case 5 -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
            default -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
        };
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(ev, (float) (pitch.value / 100.0), (float) (volume.value / 100.0)));
    }
}
