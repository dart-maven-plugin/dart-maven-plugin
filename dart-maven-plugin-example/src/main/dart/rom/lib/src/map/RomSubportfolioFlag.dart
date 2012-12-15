part of map;

class RomSubportfolioFlag extends RomFlag {

  RomSubportfolio _subportfolio;

  RomSubportfolioFlag(var subportfolioFlagGroupTag,
                      RomMap map,
                      RomSubportfolio subportfolio) : super(subportfolioFlagGroupTag, map) {
    _subportfolio = subportfolio;
  }

  bool isVisible() {
    return _subportfolio.isRootSubportfolio() || _subportfolio.expanded;
  }

  void handleOnClick(final Event e) {
    _subportfolio.fireSelctionEvent();
  }
}