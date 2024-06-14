/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.horcrux.svg

import com.facebook.react.TurboReactPackage
import com.facebook.react.ViewManagerOnDemandReactPackage
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.ModuleSpec
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.common.MapBuilder
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.module.annotations.ReactModuleList
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.uimanager.ViewManager
import com.horcrux.svg.RenderableViewManager.CircleViewManager
import com.horcrux.svg.RenderableViewManager.ClipPathViewManager
import com.horcrux.svg.RenderableViewManager.DefsViewManager
import com.horcrux.svg.RenderableViewManager.EllipseViewManager
import com.horcrux.svg.RenderableViewManager.ForeignObjectManager
import com.horcrux.svg.RenderableViewManager.GroupViewManager
import com.horcrux.svg.RenderableViewManager.ImageViewManager
import com.horcrux.svg.RenderableViewManager.LineViewManager
import com.horcrux.svg.RenderableViewManager.LinearGradientManager
import com.horcrux.svg.RenderableViewManager.MarkerManager
import com.horcrux.svg.RenderableViewManager.MaskManager
import com.horcrux.svg.RenderableViewManager.PathViewManager
import com.horcrux.svg.RenderableViewManager.PatternManager
import com.horcrux.svg.RenderableViewManager.RadialGradientManager
import com.horcrux.svg.RenderableViewManager.RectViewManager
import com.horcrux.svg.RenderableViewManager.SymbolManager
import com.horcrux.svg.RenderableViewManager.TSpanViewManager
import com.horcrux.svg.RenderableViewManager.TextPathViewManager
import com.horcrux.svg.RenderableViewManager.TextViewManager
import com.horcrux.svg.RenderableViewManager.UseViewManager
import javax.annotation.Nonnull
import javax.inject.Provider

@ReactModuleList(nativeModules = [SvgViewModule::class, RNSVGRenderableManager::class])
class SvgPackage() : TurboReactPackage(), ViewManagerOnDemandReactPackage {
    private var mViewManagers: Map<String, ModuleSpec>? = null
    private fun getViewManagersMap(reactContext: ReactApplicationContext): Map<String, ModuleSpec>? {
        if (mViewManagers == null) {
            val specs: MutableMap<String, ModuleSpec> = MapBuilder.newHashMap()
            specs.put(
                    GroupViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return GroupViewManager()
                                }
                            }))
            specs.put(
                    PathViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return PathViewManager()
                                }
                            }))
            specs.put(
                    CircleViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return CircleViewManager()
                                }
                            }))
            specs.put(
                    EllipseViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return EllipseViewManager()
                                }
                            }))
            specs.put(
                    LineViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return LineViewManager()
                                }
                            }))
            specs.put(
                    RectViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return RectViewManager()
                                }
                            }))
            specs.put(
                    TextViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return TextViewManager()
                                }
                            }))
            specs.put(
                    TSpanViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return TSpanViewManager()
                                }
                            }))
            specs.put(
                    TextPathViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return TextPathViewManager()
                                }
                            }))
            specs.put(
                    ImageViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return ImageViewManager()
                                }
                            }))
            specs.put(
                    ClipPathViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return ClipPathViewManager()
                                }
                            }))
            specs.put(
                    DefsViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return DefsViewManager()
                                }
                            }))
            specs.put(
                    UseViewManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return UseViewManager()
                                }
                            }))
            specs.put(
                    SymbolManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return SymbolManager()
                                }
                            }))
            specs.put(
                    LinearGradientManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return LinearGradientManager()
                                }
                            }))
            specs.put(
                    RadialGradientManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return RadialGradientManager()
                                }
                            }))
            specs.put(
                    PatternManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return PatternManager()
                                }
                            }))
            specs.put(
                    MaskManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return MaskManager()
                                }
                            }))
            specs.put(
                    ForeignObjectManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return ForeignObjectManager()
                                }
                            }))
            specs.put(
                    MarkerManager.Companion.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return MarkerManager()
                                }
                            }))
            specs.put(
                    SvgViewManager.REACT_CLASS,
                    ModuleSpec.viewManagerSpec(
                            object : Provider<NativeModule> {
                                public override fun get(): NativeModule {
                                    return SvgViewManager()
                                }
                            }))
            mViewManagers = specs
        }
        return mViewManagers
    }

    /** {@inheritDoc}  */
    public override fun getViewManagerNames(reactContext: ReactApplicationContext): List<String> {
        return ArrayList(getViewManagersMap(reactContext)!!.keys)
    }

    override fun getViewManagers(reactContext: ReactApplicationContext): List<ModuleSpec> {
        return ArrayList(getViewManagersMap(reactContext)!!.values)
    }

    /** {@inheritDoc}  */
    public override fun createViewManager(
            reactContext: ReactApplicationContext, viewManagerName: String): ViewManager<*, *>? {
        val spec: ModuleSpec? = getViewManagersMap(reactContext)!!.get(viewManagerName)
        return if (spec != null) spec.getProvider().get() as ViewManager<*, *>? else null
    }

    public override fun getModule(name: String, @Nonnull reactContext: ReactApplicationContext): NativeModule? {
        when (name) {
            SVG_VIEW_MODULE_NAME -> return SvgViewModule(reactContext)
            SVG_RENDERABLE_MODULE_NAME -> return RNSVGRenderableManager(reactContext)
            else -> return null
        }
    }

    public override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
        try {
            val reactModuleInfoProviderClass: Class<*> = Class.forName("com.horcrux.svg.SvgPackage$\$ReactModuleInfoProvider")
            return reactModuleInfoProviderClass.newInstance() as ReactModuleInfoProvider
        } catch (e: ClassNotFoundException) {
            // ReactModuleSpecProcessor does not run at build-time. Create this ReactModuleInfoProvider by
            // hand.
            return object : ReactModuleInfoProvider {
                public override fun getReactModuleInfos(): Map<String, ReactModuleInfo> {
                    val reactModuleInfoMap: MutableMap<String, ReactModuleInfo> = HashMap()
                    val moduleList: Array<Class<out NativeModule>> = arrayOf(
                            SvgViewModule::class.java, RNSVGRenderableManager::class.java)
                    for (moduleClass: Class<out NativeModule> in moduleList) {
                        val reactModule: ReactModule = moduleClass.getAnnotation(ReactModule::class.java)
                        reactModuleInfoMap.put(
                                reactModule.name,
                                ReactModuleInfo(
                                        reactModule.name,
                                        moduleClass.getName(),
                                        reactModule.canOverrideExistingModule,
                                        reactModule.needsEagerInit,
                                        reactModule.hasConstants,
                                        reactModule.isCxxModule,
                                        true))
                    }
                    return reactModuleInfoMap
                }
            }
        } catch (e: InstantiationException) {
            throw RuntimeException(
                    "No ReactModuleInfoProvider for MyPackage$\$ReactModuleInfoProvider", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(
                    "No ReactModuleInfoProvider for MyPackage$\$ReactModuleInfoProvider", e)
        }
    }

    @Suppress("unused")
    fun createJSModules(): List<Class<out JavaScriptModule?>> {
        return emptyList()
    }
}
