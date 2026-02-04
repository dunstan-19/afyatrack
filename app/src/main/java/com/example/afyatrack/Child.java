package com.example.afyatrack;

// Child class for spinner
    public class Child {
        private String id;
        private String name;
        private String dob;

        public Child(String id, String name, String dob) {
            this.id = id;
            this.name = name;
            this.dob = dob;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDob() { return dob; }

        @Override
        public String toString() {
            return name;
        }
    }
