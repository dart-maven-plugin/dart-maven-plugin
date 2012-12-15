part of map;

abstract class Expandable {

  bool _expanded = false;

  void expand();

  void collapse();

  bool isExpanded() => _expanded;
}