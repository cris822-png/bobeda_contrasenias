/// A single vault entry returned from the server (password already decrypted).
class VaultEntry {
  final String id;
  final String title;
  final String? username;
  final String password;   // decrypted — keep in memory only
  final DateTime createdAt;
  final DateTime updatedAt;

  const VaultEntry({
    required this.id,
    required this.title,
    this.username,
    required this.password,
    required this.createdAt,
    required this.updatedAt,
  });

  factory VaultEntry.fromJson(Map<String, dynamic> json) {
    return VaultEntry(
      id: json['id'] as String,
      title: json['title'] as String,
      username: json['username'] as String?,
      password: json['password'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
    );
  }

  Map<String, dynamic> toJson() => {
        'title': title,
        'username': username,
        'password': password,
      };
}
