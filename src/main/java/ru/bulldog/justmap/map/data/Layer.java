package ru.bulldog.justmap.map.data;

public enum Layer {
	SURFACE("surface", 256),
	CAVES("caves", 8),
	NETHER("nether", 16);

	public final String name;
	public final int height;

	private Layer(String name, int height) {
		this.name = name;
		this.height = height;
	}

	@Override
	public String toString() {
		return this.name.toUpperCase();
	}
}
