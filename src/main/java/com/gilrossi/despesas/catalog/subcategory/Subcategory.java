package com.gilrossi.despesas.catalog.subcategory;

public class Subcategory {

	private Long id;
	private Long categoryId;
	private String name;
	private boolean active;

	public Subcategory() {
	}

	public Subcategory(Long id, Long categoryId, String name, boolean active) {
		this.id = id;
		this.categoryId = categoryId;
		this.name = name;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
