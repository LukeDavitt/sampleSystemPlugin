package com.morpheusdata.omega.system

import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.providers.SystemProvider
import com.morpheusdata.model.Icon
import com.morpheusdata.model.system.SystemComponentType
import com.morpheusdata.model.system.SystemType
import com.morpheusdata.model.system.SystemTypeLayout

class DummySystemProvider implements SystemProvider {
	static final String SYSTEM_TYPE_CODE = 'dummy-type'
	static final String SYSTEM_LAYOUT_CODE = 'dummy-type-default-layout'

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
	Collection<SystemComponentType> getSystemComponentTypes() { return [] }

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
		layout.components = []
		return [layout]
	}
}
