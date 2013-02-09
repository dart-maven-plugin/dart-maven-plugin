import 'dart:core';
import 'dart:html';

//import 'package:log4dart/log4dart.dart';

import 'package:rom/components.dart';
import 'package:rom/map.dart';

void main() {
  var mapPage = new RomPage();
  var mapTags = queryAll(".map");
  for (var mapTag in mapTags) {
    var map = new RomMap(mapTag);
    map.addSelectListener(mapPage);
    map.render();
  }
}

class RomPage implements SelectionChangeListener {

  DisableButton collapseButton;
  DisableButton expandButton;

  RomPage() {
    collapseButton = new DisableButton("#action_collaps");
    expandButton = new DisableButton("#action_expand");
  }

  void selectionChanged(final selection) {
    if (selection is Expandable) {
      print("Selection expandable: ${selection}");
      changeExpandState(selection);
    } else {
      print("Selection NOT expandable: ${selection}");
    }
  }

  void changeExpandState(final Expandable expandable) {
    if (expandable.isExpanded()) {
      collapseButton.activate();
      expandButton.deactivate();
    } else {
      collapseButton.deactivate();
      expandButton.activate();
    }
  }
}