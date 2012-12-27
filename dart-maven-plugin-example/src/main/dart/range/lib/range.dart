library range;

List<int> range(int start, int stop, [int step=1]) {
  if (start >= stop) {
    throw new ArgumentError("start must be less than stop");
  }

  List<int> list = [];

  for (var i = start; i < stop; i += step) {
    list.add(i);
  }

  return list;
}