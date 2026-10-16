package flaxbeard.cyberware.common.registry;

import flaxbeard.cyberware.CyberwareMod;
import flaxbeard.cyberware.common.menu.BlueprintArchiveMenu;
import flaxbeard.cyberware.common.menu.ComponentBoxMenu;
import flaxbeard.cyberware.common.menu.EngineeringTableMenu;
import flaxbeard.cyberware.common.menu.ScannerMenu;
import flaxbeard.cyberware.common.menu.SurgeryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CyberwareMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, CyberwareMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<SurgeryMenu>> SURGERY = MENUS.register("surgery",
            () -> IMenuTypeExtension.create(SurgeryMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<EngineeringTableMenu>> ENGINEERING_TABLE = MENUS.register("engineering_table",
            () -> IMenuTypeExtension.create(EngineeringTableMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<ScannerMenu>> SCANNER = MENUS.register("scanner",
            () -> IMenuTypeExtension.create(ScannerMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<BlueprintArchiveMenu>> BLUEPRINT_ARCHIVE = MENUS.register("blueprint_archive",
            () -> IMenuTypeExtension.create(BlueprintArchiveMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<ComponentBoxMenu>> COMPONENT_BOX = MENUS.register("component_box",
            () -> IMenuTypeExtension.create(ComponentBoxMenu::fromNetwork));

    private CyberwareMenus() {
    }
}
