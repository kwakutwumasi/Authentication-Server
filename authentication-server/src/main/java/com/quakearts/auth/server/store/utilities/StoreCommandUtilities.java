package com.quakearts.auth.server.store.utilities;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

import org.infinispan.Cache;

import com.quakearts.auth.server.store.impl.RegistryStoreManagerImpl;
import com.quakearts.utilities.Command;
import com.quakearts.utilities.CommandParameter;
import com.quakearts.utilities.annotation.CommandMetadata;
import com.quakearts.utilities.annotation.CommandParameterMetadata;
import com.quakearts.utilities.exception.CommandParameterException;

@CommandMetadata(value="storeutilities", description="Utility for reading authentication server store contents",
		parameters= {@CommandParameterMetadata(value=StoreCommandUtilities.STORE, alias="s",
			format="storename",
			description="The store to read. Must be one of Registrations, Aliases and Secrets"),
		@CommandParameterMetadata(value=StoreCommandUtilities.LIST, alias="l",
			format="all|key|value", description="One of all, key or value. The type of data to show."
					+ " all lists key and value pairs. key lists only keys. value lists only values"),
		@CommandParameterMetadata(value=StoreCommandUtilities.KEY, alias="k",
			linkedParameters="store", format="keyname",
			description="The key of the entry to display")})
public class StoreCommandUtilities implements Command {
	
	protected static final String STORE = "store";
	protected static final String KEY = "key";
	protected static final String LIST = "list";
	private Map<String, CommandParameter> commandParameterMap;
	private boolean indent = false;
	private RegistryStoreManagerImpl impl = new RegistryStoreManagerImpl();
	private StringBuilder output = new StringBuilder();
	
	@Override
	public void setCommandParametersMap(Map<String, CommandParameter> commandParameterMap) {
		this.commandParameterMap = commandParameterMap;
	}

	@Override
	public void execute() throws CommandParameterException {
		if(commandParameterMap.containsKey(STORE)) {
			switch (commandParameterMap.get(STORE).getValue()) {
			case "Registrations":
				executeOn(impl.getCache());
				break;
			case "Aliases":
				executeOn(impl.getAliasCache());
				break;
			case "Secrets":
				executeOn(impl.getSecretsCache());
				break;
			default:
				throw new CommandParameterException("Invalid value: "+commandParameterMap.get(STORE), STORE);
			}
		} else {
			indent=true;
			output.append("Registrations:\n");
			executeOn(impl.getCache());
			output.append("Aliases:\n");
			executeOn(impl.getAliasCache());
			output.append("Secrets:\n");
			executeOn(impl.getSecretsCache());
		}
		System.out.print(output);
	}

	private void executeOn(Cache<String, ?> cache) throws CommandParameterException {
		if(commandParameterMap.containsKey(LIST)) {
			switch (commandParameterMap.get(LIST).getValue()) {
			case "all":
				listAllIn(cache.entrySet());
				break;
			case KEY:
				listAllIn(cache.keySet());
				break;
			case "value":
				listAllIn(cache.values());
				break;
			default:
				throw new CommandParameterException("Invalid value: "+commandParameterMap.get(LIST), LIST);
			}
		} else if(commandParameterMap.containsKey(KEY)) {
			output.append(cache.get(commandParameterMap.get(KEY).getValue())).append("\n");
		} else {
			output.append((indent?"\t":"")).append(MessageFormat.format("Size: {0}", cache.size())).append("\n");
		}
	}
	
	private void listAllIn(Collection<?> set) {
		set.stream().map(o->(indent?"\t":"")+o.toString()).forEach(this::appendWithNewLine);
	}
	
	private void appendWithNewLine(Object object) {
		output.append(object).append("\n");
	}
}
