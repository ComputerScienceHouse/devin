class DrinkMachine {
  DrinkMachine({
    required this.id,
    required this.display_name,
    required this.is_online,
    required this.name,
    required this.slots,
  });

  final int id;
  final String display_name;
  final bool is_online;
  final String name;
  final List<MachineSlot> slots;

  factory DrinkMachine.fromJson(Map<String, dynamic> json) => DrinkMachine(
        id: json["id"],
        display_name: json["display_name"],
        is_online: json["is_online"],
        name: json["name"],
        slots: List<MachineSlot>.from(
            json["slots"].map((x) => MachineSlot.fromJson(x))),
      );
}

class MachineSlot {
  MachineSlot({
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
  DrinkItem({
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
