part of map;

abstract class RomMapContainerItem extends RomMapItem implements Expandable {

  bool _expanded = false;

  RomMapContainerItem(var mapItemGroupTag, RomMap map) : super(mapItemGroupTag, map);

  void render() {
    super.render();
    renderChildren();
  }

  void expand() => expanded = true;

  void collapse() => expanded = false;

  bool isExpanded() => _expanded;

   void renderChildren();

   bool isVisible();

  void handleOnClick(final Event e) {
  }

}