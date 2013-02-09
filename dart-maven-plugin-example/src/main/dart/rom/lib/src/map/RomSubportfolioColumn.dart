part of map;

class RomSubportfolioColumn extends RomColumn {

  RomSubportfolio _subportfolio;

  RomSubportfolioColumn(var subportfolioColumnGroupTag,
                        RomMap map,
                        RomSubportfolio subportfolio) : super(subportfolioColumnGroupTag, map) {
    _subportfolio = subportfolio;
  }

  bool isVisible() {
    return _subportfolio.expanded;
  }

  void handleOnClick(final Event e) {
    _subportfolio.fireSelctionEvent();
  }

}