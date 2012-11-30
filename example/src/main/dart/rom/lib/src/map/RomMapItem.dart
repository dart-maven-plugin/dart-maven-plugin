part of map;

abstract class RomMapItem {

  static const String _HIDDEN_CLASS = 'hidden';

  final GElement _mapItemGroupTag;
  final RomMap _map;

  RomMapItem(this._mapItemGroupTag, this._map) {
    assert(_mapItemGroupTag != null);
    assert(_map != null);
    _mapItemGroupTag.on.click.add(handleOnClick);
  }

  void render() {
    if (isVisible()) {
      _mapItemGroupTag.classes.remove(_HIDDEN_CLASS);
    } else {
      _mapItemGroupTag.classes.add(_HIDDEN_CLASS);
    }
  }

  bool isVisible();

  void handleOnClick(final Event e) {
    _map.fireSlectionChangeEvent(this);
  }

}