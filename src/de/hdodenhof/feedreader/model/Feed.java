package de.hdodenhof.feedreader.model;

public class Feed {
        private long mID;
        private String mName;
        private String mURL;

        public long getId() {
                return mID;
        }

        public void setId(long id) {
                this.mID = id;
        }

        public String getUrl() {
                return mURL;
        }

        public void setUrl(String comment) {
                this.mURL = comment;
        }

        public String getName() {
                return mName;
        }

        public void setName(String name) {
                this.mName = name;
        }

        @Override
        public String toString() {
                return getName();
        }
}
