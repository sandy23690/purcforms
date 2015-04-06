This is GWT based form designer and form runtime engine.
The form definitions are stored as xforms and the layout (position, size, etc) of input widgets (textboxes, buttons, etc) is stored in a custom XML format. One can use the form designer and form runtime engine separately. Incorporating any of the two in your application requires just referencing one JavaScript file and having a few other dependant files on your server. The form designer also has an html file that you can run in the browser without requiring a server.

You can test the demo form designer instance at: http://purcforms.appspot.com/FormDesigner.html

The form run time engine is also used to create a FormEntry widget which can be used for data entry in offline mode. The connection to the server is needed only when downloading forms and sending collected data back to the server. During filling of forms, they are saved on the local computer. With an HTML5 enabled browser, you should be able to access the FormEntry widget even without a server connection.

You can try out the demo at: http://purcforms.appspot.com/FormEntry.html


For GWT applications that want to use the form designer, form runner or form entry widgets, you do not need the generated JavaScript file, you can just use the widget jar file.