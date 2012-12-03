part of map;

class RomSubportfolio extends RomMapContainerItem {

  static const String SUBPORTFOLIO_FLAG_CLASS = "g.subportfolioFlag";
  static const String SUBPORTFOLIO_COLUMN_CLASS = "g.subportfolioColumn";

  RomSubportfolio _parentSubportfolio;
  RomPortfolio _portfolio;

  RomSubportfolioFlag _subportfolioFlag;
  RomSubportfolioColumn _subportfolioColumn;


  RomSubportfolio.childOfPortfolio(var subportfolioGroupTag,
                                   RomMap map,
                                   RomPortfolio portfolio) : super (subportfolioGroupTag, map) {
    _parentSubportfolio = null;
    _portfolio = portfolio;
    _init();
  }

  RomSubportfolio.childOfSubportfolio(var subportfolioGroupTag,
                                      RomMap map,
                                      RomPortfolio portfolio,
                                      RomSubportfolio parentSubportfolio) : super (subportfolioGroupTag, map) {
    _parentSubportfolio = parentSubportfolio;
    _portfolio = portfolio;
    _init();
  }

  void _init() {
    var subportfolioFlagGroupTag = _mapItemGroupTag.query(SUBPORTFOLIO_FLAG_CLASS);
    _subportfolioFlag = new RomSubportfolioFlag(subportfolioFlagGroupTag, _map, this);

    if (!isRootSubportfolio()) {
      var subportfolioColumnGroupTag = _mapItemGroupTag.query(SUBPORTFOLIO_COLUMN_CLASS);
      _subportfolioColumn = new RomSubportfolioColumn(subportfolioColumnGroupTag, _map, this);
    }
  }

  void renderChildren() {
    _subportfolioFlag.render();
    if (!isRootSubportfolio()) {
      _subportfolioColumn.render();
    }
  }

  bool isVisible() {
    if (isRootSubportfolio()) {
      return _portfolio.isVisible();
    } else {
      return _parentSubportfolio.isVisible();
    }
  }

  bool isRootSubportfolio() {
    return _parentSubportfolio == null;
  }

  void fireSelctionEvent() {
    _map.fireSlectionChangeEvent(this);
  }

}