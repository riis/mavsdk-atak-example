MavSDK ATAK Plugin Example


_________________________________________________________________
PURPOSE AND CAPABILITIES

This plugin showcases a modern (5.5) ATAK plugin using the new recommended formatting provided by TAK


_________________________________________________________________
STATUS

Demo Project

_________________________________________________________________
POINT OF CONTACTS

- Godfrey Nolan
- Zain Raza

_________________________________________________________________
PORTS REQUIRED

udp://:14550

_________________________________________________________________
EQUIPMENT REQUIRED

- Docker

_________________________________________________________________
EQUIPMENT SUPPORTED

_________________________________________________________________
COMPILATION

## Compilation Instructions

_________________________________________________________________
DEVELOPER NOTES

## How is this structure from the old one?

As you'll notice, the key difference is the lack of a `MapComponent` and `DropDownReceiver` classes. Previously we would treat the `MapComponent` as the entrypoint for the plugin and use it to control the map. Now we just have the default `PluginTemplate` class which we can rename to our own plugin name. The `PluginTemplate` class is the entrypoint for the plugin and is where we can initialize our plugin and set up any necessary components.

We can now use the `PluginTemplate` and the `IHostUIService` to implement our designs in the ATAK UI. We can inspect the following snippet to understand the kind of control we now have over the ATAK UI:

```kotlin
// obtain the UI service
uiService = serviceController.getService<IHostUIService?>(IHostUIService::class.java)

// initialize the toolbar button for the plugin

// create the button
toolbarItem = ToolbarItem.Builder(
    pluginContext!!.getString(R.string.app_name),
    MarshalManager.marshal<Bitmap?, Drawable?>(
        pluginContext!!.getResources().getDrawable(R.drawable.ic_launcher),
            Drawable::class.java,
            Bitmap::class.java
        )
    )
    .setListener(object : ToolbarItemAdapter() {
        override fun onClick(item: ToolbarItem?) {
            showPane()
        }
    })
    .build()
```

This snippet means that when the user clicks on the toolbar button, the `showPane()` method will be called. This is where we can implement our custom UI logic if we so choose.

We can also declare our own UI classes and views (similar to previous `DropDownReceivers`) so we can display them:

```kotlin
val view = PluginLayoutInflater.inflate(
                pluginContext,
                R.layout.main_layout,
                null
            )
            positionTextView = view.findViewById(R.id.positionTextView)

            templatePane = PaneBuilder(view) // relative location is set to default; pane will switch location dependent on
                // current orientation of device screen
                .setMetaValue(
                    Pane.RELATIVE_LOCATION,
                    Pane.Location.Default
                ) // pane will take up 50% of screen width in landscape mode
                .setMetaValue(
                    Pane.PREFERRED_WIDTH_RATIO,
                    0.5
                ) // pane will take up 50% of screen height in portrait mode
                .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5)
                .build()
```

Here we are inflating a layout from the resources and setting it up as a pane. The `PaneBuilder` allows us to specify how the pane should be displayed, including its size and position on the screen. We could've expanded our view into it's own class, but for simplicity we are just using the `PluginLayoutInflater` to inflate the layout directly in the `PluginTemplate` class. We can then just use the default `showPane()` function to show our custom view.

If I wanted to create another view in the same way, I could create another XML layout file and switch between them in the `showPane()` method. This allows us to have multiple views and switch between them as needed. At that point I would likely be creating a new class for each view to keep things organized, but for simplicity's sake I've left it in `PluginTemplate`.