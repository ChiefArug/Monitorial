# Monitorial
#### Get that Minecraft window to open where you want it!


Monitorial is a responsive, intuitive and highly configurable mod that makes the Minecraft window open where you want it, and not anywhere else. By default it is configured to be in Automatic mode, which will remember the last monitor, size and position you put the Minecraft window on and restore it to that next launch, however it can be configured to go to a set position and size instead if you wish. The configuration screen has a lot of information in the form of tooltips and is very intuitive and responsive, so you should just be able to jump into it and start configuring. Note that by default the configuration is shared between all instances, change this by clicking the Currently Editing: Global button.


If you do want more information about how the configuration works read on:


### useGlobalConfig
  By default, Monitorial uses a global configuration file, which is saved in your .minecraft directory and will apply to all instances that have Monitorial installed. If you do not wish to have this shared between instances then you can either set the global config to use local configuration, which will cause all instances to use their local configuration, or you can just set the current local configuration to use the local configuration (which will also prevent Monitorial from attempting to load the global config file at all for that instance, if that is causing issues). If you wish to know exactly where a config is stored check the tooltip of the Currently Editing button in the configuration
### automaticMode
  By default Monitorial runs in Automatic mode, and will remember the last window position and restore the game window to there on next launch. This can be disabled which allows for manual configuration of the monitor, size and position.
### defaultMonitor
  The monitor that the Minecraft window should appear on. If this is not present then the primary monitor is used. In the configuration file this stores both the monitors name and position, as multiple monitors can have the same name. The position is also used as a fallback if no monitor with that name is found.
### forceMove
  If Monitorial should attempt to move and resize the window after it has opened. Note that this behaviour is not supported on all window managers, but this is required to be enabled if you want to customize the window position and not just the monitor it appears on. If you have NeoForge's Early Loading Screen enabled then this needs to be enabled for Monitorial to have any affect on the game window. Can be set to ELS_ONLY which will only try and force move it if the Early Loading Screen is enabled.
### position
  The x and y offset from the top left corner of the desired monitor.
### windowSize
  The x (width and y (height) size of the window.
