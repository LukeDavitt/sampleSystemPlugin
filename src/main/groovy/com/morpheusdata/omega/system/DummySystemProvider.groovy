package com.morpheusdata.omega.system

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.providers.SystemProvider
import com.morpheusdata.model.ComputeServer
import com.morpheusdata.model.Icon
import com.morpheusdata.model.StorageServer
import com.morpheusdata.model.system.System
import com.morpheusdata.model.system.SystemComponent
import com.morpheusdata.model.system.SystemComponentType
import com.morpheusdata.model.system.SystemRequest
import com.morpheusdata.model.system.SystemType
import com.morpheusdata.model.system.SystemTypeLayout
import com.morpheusdata.response.ServiceResponse

class DummySystemProvider implements SystemProvider {
	static final String SYSTEM_TYPE_CODE = 'dummy-type'
	static final String SYSTEM_LAYOUT_CODE = 'dummy-type-default-layout'

	// refType values written onto SystemComponent when linked to a real resource
	static final String REF_TYPE_COMPUTE_SERVER = 'ComputeServer'
	static final String REF_TYPE_STORAGE_SERVER = 'StorageServer'

	Plugin plugin
	MorpheusContext morpheusContext

	DummySystemProvider(Plugin plugin, MorpheusContext context) {
		this.plugin = plugin
		this.morpheusContext = context
	}

	@Override
	MorpheusContext getMorpheus() { return morpheusContext }

	@Override
	String getCode() { return SYSTEM_TYPE_CODE }

	@Override
	String getName() { return 'Dummy System Type' }

	@Override
	String getDescription() { return 'Test system provider' }

	@Override
	Icon getIcon() { return null }

	@Override
	Collection<SystemComponentType> getSystemComponentTypes() {
		def computeNode = new SystemComponentType()
		computeNode.code = 'dummy-compute-node'
		computeNode.name = 'Compute Node'
		computeNode.description = 'A compute node in the system'
		computeNode.category = 'compute'
		computeNode.active = true
		computeNode.modelType = ComputeServer

		def storageController = new SystemComponentType()
		storageController.code = 'dummy-storage-controller'
		storageController.name = 'Storage Controller'
		storageController.description = 'A storage controller in the system'
		storageController.category = 'storage'
		storageController.active = true
		storageController.modelType = StorageServer

		def networkSwitch = new SystemComponentType()
		networkSwitch.code = 'dummy-network-switch'
		networkSwitch.name = 'Network Switch'
		networkSwitch.description = 'A network switch in the system'
		networkSwitch.category = 'network'
		networkSwitch.active = true
		networkSwitch.modelType = ComputeServer

		return [computeNode, storageController, networkSwitch]
	}

	@Override
	Collection<SystemType> getSystemTypes() {
		def systemType = new SystemType()
		systemType.code = SYSTEM_TYPE_CODE
		systemType.name = 'Dummy System Type'
		systemType.description = 'Test system type for SystemsService CRUD validation'
		systemType.active = true
		systemType.creatable = true
		systemType.editable = true
		return [systemType]
	}

	@Override
	Collection<SystemTypeLayout> getSystemTypeLayouts() {
		def systemType = getSystemTypes().first()
		def layout = new SystemTypeLayout()
		layout.code = SYSTEM_LAYOUT_CODE
		layout.name = 'Dummy Default Layout'
		layout.description = 'Default layout for the dummy system type'
		layout.version = '1.0'
		layout.enabled = true
		layout.systemType = systemType
		layout.components = getSystemComponentTypes()
		return [layout]
	}

	/**
	 * Called during system initialization for each component that has not yet been linked
	 * to a real resource (externalId is null). Reads the server id from configOptions
	 * (keyed by component type code), sets refType/refId on the matching SystemComponent,
	 * and persists it so the appliance can resolve the link for update definition lookups.
	 *
	 * Expected configOptions keys:
	 *   - 'dummy-compute-node'    => id of the ComputeServer to link
	 *   - 'dummy-storage-controller' => id of the StorageServer to link
	 */
	@Override
	ServiceResponse addSystemComponent(System system, SystemRequest systemRequest, SystemComponentType componentType) {
		try {
			def resourceId = systemRequest.getConfigOption(componentType.code)
			if (!resourceId) {
				// No resource id provided for this component type — nothing to link
				return ServiceResponse.success()
			}

			String refType = resolveRefType(componentType)
			if (!refType) {
				// No known resource mapping for this component type (e.g. network switch)
				return ServiceResponse.success()
			}

			// Find the unlinked SystemComponent of this type on the system
			SystemComponent component = system.components?.find { it.type?.code == componentType.code && !it.externalId }
			if (!component) {
				return ServiceResponse.error("No unlinked component found for type '${componentType.code}' on system '${system.name}'")
			}

			component.system = system
			component.refType = refType
			component.refId = resourceId.toString()
			component.externalId = resourceId.toString()
			morpheusContext.async.system.component.save(component).blockingGet()

			return ServiceResponse.success()
		} catch (Exception e) {
			return ServiceResponse.error("Failed to add system component '${componentType.code}': ${e.message}")
		}
	}

	/**
	 * Called during system update for components that are already linked (externalId is set).
	 * Re-links if a new resource id is provided in configOptions, otherwise leaves unchanged.
	 */
	@Override
	ServiceResponse updateSystemComponent(System system, SystemRequest systemRequest, SystemComponentType componentType) {
		try {
			def resourceId = systemRequest.getConfigOption(componentType.code)
			if (!resourceId) {
				return ServiceResponse.success()
			}

			String refType = resolveRefType(componentType)
			if (!refType) {
				return ServiceResponse.success()
			}

			SystemComponent component = system.components?.find { it.type?.code == componentType.code }
			if (!component) {
				return ServiceResponse.error("No component found for type '${componentType.code}' on system '${system.name}'")
			}

			component.system = system
			component.refType = refType
			component.refId = resourceId.toString()
			component.externalId = resourceId.toString()
			morpheusContext.async.system.component.save(component).blockingGet()

			return ServiceResponse.success()
		} catch (Exception e) {
			return ServiceResponse.error("Failed to update system component '${componentType.code}': ${e.message}")
		}
	}

	private static String resolveRefType(SystemComponentType componentType) {
		if (componentType.modelType == ComputeServer) return REF_TYPE_COMPUTE_SERVER
		if (componentType.modelType == StorageServer) return REF_TYPE_STORAGE_SERVER
		return null
	}
}
