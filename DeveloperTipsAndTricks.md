# Introduction #

Given below are some tips and tricks for development with Purcforms


# Details #

1. If you are opening a filled form (with formData xml), then these values are loaded into defaultValues of the questionDef.
So if suppose you were to create new widget, then this widget will reload older data from defaultValue. (For reference, goto: RunTimeWidgetWrapper.java method: loadQuestion())

2. There have been changes done to TreeItem.java and Tree.java from gwt-user.jar. These changes work on purcforms because purcforms has the necessary code. But if you were to integrate the purcforms jar file into something else, then the compiler may get confused between which classes to load, purcforms one or gwt-user ones. To get rid of this problem, one way is to put the 2 classes into your code so that it automatically gets priority and integration goes smoothly. (Trying to extend the classes or changing the name of the classes didn't work and hence one has to do this as the last ditch effort)

3. If you are trying to integrate Purcforms into your code, there could be an issue when certain popups and suggestion boxes start disappearing. This problem is seen when you are creating a GXT application and using Purcforms in it. This happens because the GXT widgets get higher priority in loading and the popups are left in the background and hence the functionality doesn't seem to work properly. This problem, though daunting, is incredibly easy to fix. For the particular missing GWT pop-up widget, you can add something like this in your css file:
```
.gwt-DialogBox {
  z-index: 99999;
}
```

In this example, it was assumed that the gwt-DialogBox was coming in the background. What we just did here was to give z-index a very high value so that it is displayed over other windows. (Note:z index should be high positive value, -99999 will reverse the effect, and put the widget at the bottom of all popup panels)