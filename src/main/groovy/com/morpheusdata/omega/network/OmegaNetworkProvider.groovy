package com.morpheusdata.omega.network

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.data.DataQuery
import com.morpheusdata.core.providers.AbstractNetworkProvider
import com.morpheusdata.core.providers.NetworkProvider
import com.morpheusdata.model.Icon
import com.morpheusdata.model.Network
import com.morpheusdata.model.NetworkSubnet
import com.morpheusdata.model.NetworkRouterType
import com.morpheusdata.model.NetworkServer
import com.morpheusdata.model.NetworkType
import com.morpheusdata.model.OptionType
import com.morpheusdata.model.UpdateDefinition
import com.morpheusdata.model.UpdateOperation
import com.morpheusdata.response.ServiceResponse

class OmegaNetworkProvider extends AbstractNetworkProvider implements NetworkProvider, NetworkProvider.NetworkUpdateFacet {

	static final String NETWORK_PROVIDER_CODE = 'omega.network'

	protected MorpheusContext morpheusContext
	protected Plugin plugin

	OmegaNetworkProvider(Plugin plugin, MorpheusContext morpheusContext) {
		this.plugin = plugin
		this.morpheusContext = morpheusContext
	}

	@Override
	MorpheusContext getMorpheus() {
		return morpheusContext
	}

	@Override
	Plugin getPlugin() {
		return plugin
	}

	@Override
	String getCode() {
		return NETWORK_PROVIDER_CODE
	}

	@Override
	String getName() {
		return 'Omega Network Provider'
	}

	@Override
	String getDescription() {
		return 'Sample network provider for system-scoped network update testing.'
	}

	@Override
	Boolean getCreatable() {
		return true
	}

	@Override
	Boolean isUserVisible() {
		return true
	}

	@Override
	String getNetworkServerTypeCode() {
		return NETWORK_PROVIDER_CODE
	}

	@Override
	Icon getIcon() {
		return new Icon(path: 'omega.svg', darkPath: 'omega-dark.svg')
	}

	@Override
	Collection<OptionType> getOptionTypes() {
		return []
	}

	@Override
	Collection<NetworkType> getNetworkTypes() {
		return []
	}

	@Override
	Collection<NetworkRouterType> getRouterTypes() {
		return []
	}

	@Override
	ServiceResponse validateNetworkServer(NetworkServer networkServer, Map opts) {
		return ServiceResponse.success()
	}

	@Override
	ServiceResponse refresh(NetworkServer networkServer) {
		seedUpdateDefinitions(networkServer)
		return ServiceResponse.success(networkServer)
	}

	@Override
	ServiceResponse<Network> createNetwork(Network network, Map opts) {
		return ServiceResponse.success(network)
	}

	@Override
	ServiceResponse<Network> updateNetwork(Network network, Map opts) {
		return ServiceResponse.success(network)
	}

	@Override
	ServiceResponse deleteNetwork(Network network, Map opts) {
		return ServiceResponse.success()
	}

	@Override
	ServiceResponse<NetworkSubnet> createSubnet(NetworkSubnet subnet, Network network, Map opts) {
		return ServiceResponse.success(subnet)
	}

	@Override
	ServiceResponse<NetworkSubnet> updateSubnet(NetworkSubnet subnet, Network network, Map opts) {
		return ServiceResponse.success(subnet)
	}

	@Override
	ServiceResponse deleteSubnet(NetworkSubnet subnet, Network network, Map opts) {
		return ServiceResponse.success()
	}

	@Override
	ServiceResponse<UpdateOperation> validateUpdate(NetworkServer networkServer, UpdateDefinition updateDefinition) {
		return ServiceResponse.success(new UpdateOperation())
	}

	@Override
	ServiceResponse<UpdateOperation> executeUpdate(NetworkServer networkServer, UpdateDefinition updateDefinition) {
		if (!networkServer?.id) {
			return ServiceResponse.error('Network server not found for update execution')
		}

		networkServer.name = buildUpdatedNetworkServerName(networkServer, updateDefinition)
		morpheusContext.services.network.server.save(networkServer)
		return ServiceResponse.success(new UpdateOperation())
	}

	@Override
	ServiceResponse<UpdateOperation> refreshUpdate(NetworkServer networkServer, UpdateOperation updateOperation) {
		return ServiceResponse.success(updateOperation ?: new UpdateOperation())
	}

	@Override
	ServiceResponse<UpdateOperation> postUpdate(NetworkServer networkServer, UpdateDefinition updateDefinition) {
		return ServiceResponse.success(new UpdateOperation())
	}

	@Override
	ServiceResponse<UpdateOperation> rollbackUpdate(NetworkServer networkServer, UpdateDefinition updateDefinition) {
		return ServiceResponse.success(new UpdateOperation())
	}

	private String buildUpdatedNetworkServerName(NetworkServer networkServer, UpdateDefinition updateDefinition) {
		def updateLabel = updateDefinition?.name ?: 'Updated'
		return "${updateLabel} - ${networkServer.id}"
	}

	private void seedUpdateDefinitions(NetworkServer networkServer) {
		if (!networkServer?.type?.id) {
			return
		}

		Long typeId = networkServer.type.id
		def definition = [
			code: 'omega.network.update.patch',
			name: 'Omega Network Demo Update',
			version: '1.0.1',
			refType: 'NetworkServerType',
			refId: typeId,
			supportsRollback: false,
			requiresReboot: false,
			requiresRestart: false,
			requiresMaintenanceMode: false,
			isPlugin: true,
			severity: 'normal',
			type: 'enhancement',
			description: 'Harmless demo update for network server testing.',
			zeroDowntime: true
		]

		def existing = morpheusContext.services.updateDefinition.find(new DataQuery().withFilter('code', definition.code))
		if (existing) {
			def needsSave = false
			if (existing.refId != definition.refId) { existing.refId = definition.refId; needsSave = true }
			if (existing.enabled != true) { existing.enabled = true; needsSave = true }
			if (needsSave) {
				morpheusContext.services.updateDefinition.save(existing)
			}
		} else {
			morpheusContext.services.updateDefinition.create(new UpdateDefinition(
				code: definition.code,
				name: definition.name,
				version: definition.version,
				refType: definition.refType,
				refId: definition.refId,
				supportsRollback: definition.supportsRollback,
				requiresReboot: definition.requiresReboot,
				requiresRestart: definition.requiresRestart,
				requiresMaintenanceMode: definition.requiresMaintenanceMode,
				isPlugin: definition.isPlugin,
				severity: definition.severity,
				type: definition.type,
				description: definition.description,
				zeroDowntime: definition.zeroDowntime,
				enabled: true,
				updateReleaseDate: new Date()
			))
		}
	}
}
