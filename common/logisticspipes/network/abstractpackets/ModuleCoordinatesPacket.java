package logisticspipes.network.abstractpackets;

import java.io.IOException;

import logisticspipes.LogisticsPipes;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.modules.abstractmodules.LogisticsModule.ModulePositionType;
import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyModuleContainer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.EntityPlayer;

@Accessors(chain=true)
public abstract class ModuleCoordinatesPacket extends CoordinatesPacket {

	@Getter
	@Setter
	private ModulePositionType type;
	
	@Getter
	@Setter
	private int positionInt;
	
	public ModuleCoordinatesPacket(int id) {
		super(id);
	}
	
	@Override
	public void writeData(LPDataOutputStream data) throws IOException {
		super.writeData(data);
		data.writeBoolean(type != null);
		if(type != null) {
			data.writeEnum(type);
			data.writeInt(positionInt);
		}
	}

	@Override
	public void readData(LPDataInputStream data) throws IOException {
		super.readData(data);
		if(data.readBoolean()) {
			type = data.readEnum(ModulePositionType.class);
			positionInt = data.readInt();
		}
	}

	@Override
	public ModuleCoordinatesPacket setModulePos(LogisticsModule module) {
		type = module.getSlot();
		positionInt = module.getPositionInt();
		super.setModulePos(module);
		return this;
	}
	
	public ModuleCoordinatesPacket setPacketPos(ModuleCoordinatesPacket packet) {
		this.type = packet.type;
		this.positionInt = packet.positionInt;
		super.setPacketPos(packet);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getLogisticsModule(EntityPlayer player, Class<T> clazz) {
		LogisticsModule module = null;
		if(this.type == ModulePositionType.IN_PIPE) {
			LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld());
			if(pipe == null || !(pipe.pipe instanceof CoreRoutedPipe)) {
				if(LogisticsPipes.DEBUG) {
					LogisticsPipes.log.severe(this.toString());
					new RuntimeException("Couldn't find " + clazz.getName() + ", pipe didn't exsist").printStackTrace();
				}
				return null;
			}
			module = ((CoreRoutedPipe)pipe.pipe).getLogisticsModule();
		} else if (this.type == ModulePositionType.IN_HAND) {
			if(MainProxy.isServer(player.getEntityWorld())) {
				if(player.openContainer instanceof DummyModuleContainer) {
					DummyModuleContainer dummy = (DummyModuleContainer) player.openContainer;
					module = dummy.getModule();
				} else {
					if(LogisticsPipes.DEBUG) {
						LogisticsPipes.log.severe(this.toString());
						new RuntimeException("Couldn't find " + clazz.getName() + ", container wasn't a DummyModule Container").printStackTrace();
					}
					return null;
				}
			} else {
				module = MainProxy.proxy.getModuleFromGui();
				if(module == null) {
					if(LogisticsPipes.DEBUG) {
						LogisticsPipes.log.severe(this.toString());
						new RuntimeException("Couldn't find " + clazz.getName() + ", GUI didn't provide the module").printStackTrace();
					}
					return null;
				}
			}
		} else {
			LogisticsTileGenericPipe pipe = this.getPipe(player.getEntityWorld());
			if(pipe == null || !(pipe.pipe instanceof CoreRoutedPipe)) {
				if(LogisticsPipes.DEBUG) {
					LogisticsPipes.log.severe(this.toString());
					new RuntimeException("Couldn't find " + clazz.getName() + ", pipe didn't exsist").printStackTrace();
				}
				return null;
			}
			if(!(pipe.pipe instanceof PipeLogisticsChassi)) {
				if(LogisticsPipes.DEBUG) {
					LogisticsPipes.log.severe(this.toString());
					new RuntimeException("Couldn't find " + clazz.getName() + ", pipe wasn't a chassi pipe").printStackTrace();
				}
				return null;
			}
			module = ((PipeLogisticsChassi)pipe.pipe).getLogisticsModule().getSubModule(positionInt);
		}
		if(module != null) {
			if(!(clazz.isAssignableFrom(module.getClass()))) {
				if(LogisticsPipes.DEBUG) {
					LogisticsPipes.log.severe(this.toString());
					new RuntimeException("Couldn't find " + clazz.getName() + ", found " + module.getClass()).printStackTrace();
				}
				return null;
			}
		} else {
			if(LogisticsPipes.DEBUG) {
				LogisticsPipes.log.severe(this.toString());
				new RuntimeException("Couldn't find " + clazz.getName()).printStackTrace();
			}
		}
		return (T) module;
	}
}
