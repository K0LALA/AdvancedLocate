package fr.kolala.mixin;

import fr.kolala.util.IStructureStartCustomMethods;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(StructureStartMixin.class)
public abstract class StructureStartMixin implements IStructureStartCustomMethods {
    private UUID commandIdentifier = UUID.randomUUID();

    @Override
    public void advancedLocate$setCommandIdentifier(UUID uuid) {
        this.commandIdentifier = uuid;
    }

    @Override
    public boolean advancedLocate$isCommandReferenced(UUID uuid) {
        return commandIdentifier.equals(uuid);
    }
}
