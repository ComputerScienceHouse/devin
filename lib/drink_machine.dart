class DrinkMachine {
  const DrinkMachine({
    required this.id,
    required this.displayName,
    required this.isOnline,
    required this.name,
    required this.slots,
  });

  final int id;
  final String displayName;
  final bool isOnline;
  final String name;
  final List<MachineSlot> slots;

  factory DrinkMachine.fromJson(Map<String, dynamic> json) => DrinkMachine(
        id: json["id"],
        displayName: json["display_name"],
        isOnline: json["is_online"],
        name: json["name"],
        slots: List<MachineSlot>.from(
            json["slots"].map((x) => MachineSlot.fromJson(x))),
      );
}

class MachineSlot {
  const MachineSlot({
    required this.active,
    required this.count,
    required this.empty,
    required this.item,
    required this.machine,
    required this.number,
  });

  final bool active;
  final int? count;
  final bool empty;
  final DrinkItem item;
  final int machine;
  final int number;

  factory MachineSlot.fromJson(Map<String, dynamic> json) => MachineSlot(
        active: json["active"],
        count: json["count"],
        empty: json["empty"],
        item: DrinkItem.fromJson(json["item"]),
        machine: json["machine"],
        number: json["number"],
      );
}

class DrinkItem {
  const DrinkItem({
    required this.id,
    required this.name,
    required this.price,
  });

  final int id;
  final String name;
  final int price;

  factory DrinkItem.fromJson(Map<String, dynamic> json) => DrinkItem(
        id: json["id"],
        name: json["name"],
        price: json["price"],
      );
}
