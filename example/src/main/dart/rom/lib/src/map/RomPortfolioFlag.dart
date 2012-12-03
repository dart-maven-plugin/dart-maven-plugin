part of map;

class RomPortfolioFlag extends RomFlag {

  RomPortfolio _portfolio;

  RomPortfolioFlag(var portfolioFlagGroupTag, RomMap map, RomPortfolio portfolio) : super(portfolioFlagGroupTag, map) {
    _portfolio = portfolio;
  }

  bool isVisible() {
    return true;
  }

  void handleOnClick(final Event e) {
    _portfolio.fireSelctionEvent();
  }
}