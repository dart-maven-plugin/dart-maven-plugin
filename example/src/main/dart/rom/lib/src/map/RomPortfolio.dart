part of map;

class RomPortfolio extends RomMapContainerItem {

  static const String FLAG_CLASS = "g.portfolioFlag";
  static const String CASH_CLASS = "g.cashColumn";
  static const String INHOUSE_SUBPORTFOLIO_CLASS = "g.inhouseSubportfolio";
  static const String EXTERNAL_SUBPORTFOLIO_CLASS = "g.externalSubportfolio";
  static const String RECOMMENDATION_CLASS = "g.recommendationColumn";


  RomPortfolioFlag _portfolioFlag;
  RomCashColumn _cashColumn;
  RomSubportfolio _inhouseSubportfolio;
  RomSubportfolio _externalSubportfolio;
  final Set<RomRecommendationColumn> _recommendations = new Set<RomRecommendationColumn>();

  RomPortfolio(var portfolioGroupTag, RomMap map) : super(portfolioGroupTag, map) {

    this._expanded = true;

    var portfolioFlagGroupTag = _mapItemGroupTag.query(FLAG_CLASS);
    _portfolioFlag = new RomPortfolioFlag(portfolioFlagGroupTag, _map, this);

    var cashColumnGroupTag = _mapItemGroupTag.query(CASH_CLASS);
    _cashColumn = new RomCashColumn(cashColumnGroupTag, _map);

    var inhouseSubportfolioGroupTag = _mapItemGroupTag.query(INHOUSE_SUBPORTFOLIO_CLASS);
    _inhouseSubportfolio = new RomSubportfolio.childOfPortfolio(inhouseSubportfolioGroupTag, _map, this);

    var externalSubportfolioGroupTag = _mapItemGroupTag.query(EXTERNAL_SUBPORTFOLIO_CLASS);
    _externalSubportfolio = new RomSubportfolio.childOfPortfolio(externalSubportfolioGroupTag, _map, this);

    var recommendationGroupTags = _mapItemGroupTag.queryAll(RECOMMENDATION_CLASS);
    for (var recommendationGroupTag in recommendationGroupTags) {
      RomRecommendationColumn recommendation = new RomRecommendationColumn(recommendationGroupTag, _map);
      _recommendations.add(recommendation);
    }

  }

  void renderChildren() {
    _portfolioFlag.render();
    _cashColumn.render();
    _inhouseSubportfolio.render();
    _externalSubportfolio.render();
  }

  bool isVisible() {
    return true;
  }

  void fireSelctionEvent() {
    _map.fireSlectionChangeEvent(this);
  }
}