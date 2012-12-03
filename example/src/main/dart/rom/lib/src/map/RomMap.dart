part of map;

class RomMap {

//final Logger _logger;
//_logger = LoggerFactory.getLogger("RomMap");

  static const String _HIDDEN_CLASS = 'hidden';

  final SvgSvgElement _mapSvgTag;
  final Set<RomPortfolio> _portfolios = new Set<RomPortfolio>();
  final List<SelectionChangeListener> _listeners = new List<SelectionChangeListener>();

  var _selectedNode;

  RomMap(this._mapSvgTag) {
    var portfoliosGroupTags = _mapSvgTag.queryAll('g.portfolio');
    for (var portfoliosGroupTag in portfoliosGroupTags) {
      var portfolio = new RomPortfolio(portfoliosGroupTag, this);
      _portfolios.add(portfolio);
    }
  }

  void render() {
    if (isVisible()) {
      _mapSvgTag.classes.remove(_HIDDEN_CLASS);
    } else {
      _mapSvgTag.classes.add(_HIDDEN_CLASS);
    }
    for (RomPortfolio portfolio in _portfolios) {
      portfolio.render();
    }
  }

  bool isVisible() {
    return true;
  }

  void fireSlectionChangeEvent(final selectedNode) {
    _selectedNode = selectedNode;
    for (SelectionChangeListener listener in _listeners) {
      listener.selectionChanged(_selectedNode);
    }
  }

  void addSelectListener(final SelectionChangeListener listener) {
    _listeners.add(listener);
  }
}

