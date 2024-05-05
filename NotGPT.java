package prog11;

import prog08.ExternalSort;
import prog08.Sorter;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

public class NotGPT implements SearchEngine {

    public HardDisk pageDisk = new HardDisk();
    public HardDisk wordDisk = new HardDisk();
    Map<String, Long> indexOfWord = new HashMap<String,Long>();


    Map<String, String> indexOfURL = new prog09.BTree(100);

    Long indexPage(String url) {
        Long index = pageDisk.newFile();
        InfoFile newFile = new InfoFile(url);
        pageDisk.put(index, newFile);
        indexOfURL.put(url, index.toString());
        //System.out.println("indexing url " + url + " index " + index + " file " + newFile);
        return index;
    }

    Long indexWord(String URL) {
        Long index = wordDisk.newFile();
        InfoFile newFile = new InfoFile(URL);
        wordDisk.put(index, newFile);
        indexOfWord.put(URL, index);
        //System.out.println("indexing word " + wordDisk.get(index).data + " index " + index + " file " + newFile);
        return index;
    }

    @Override
    public void collect(Browser browser, List<String> startingURLs) {
        //System.out.println("starting pages " + startingURLs.get(0));
        ArrayDeque<Long> queue = new ArrayDeque<>();
        //Do the following while the queue is not empty:
        for(String s:startingURLs) {
            if(!pageDisk.containsKey(s))
                queue.offer(indexPage(s));
        }
        while(!queue.isEmpty()) {
            //System.out.println("queue" + queue);
            Long index = queue.poll();
            //System.out.println("dequeued " + pageDisk.get(index));
            if(browser.loadPage(pageDisk.get(index).data)) {
                List<String> loaded = browser.getURLs();
                for(String URL:loaded) {
                    if(!indexOfURL.containsKey(URL)) {
                        queue.offer(indexPage(URL));
                    }
                }
            }
            List<String> urls = browser.getURLs();
            Set<String> urlsOnPage = new TreeSet<>();
            for(String url:urls) {
                if(!urlsOnPage.contains(url)) {
                    urlsOnPage.add(url);
                    String w = indexOfURL.get(url);
                    if(w==null)
                        indexPage(url);
                    pageDisk.get(index).indices.add(Long.parseLong(w));
                }
            }
            Set<String> wordsOnPage = new TreeSet<>();
            List<String> words = browser.getWords();
            for(String s: words) {
                if (!indexOfWord.containsKey(s)) {
                    indexWord(s);
                }
                if(!wordsOnPage.contains(s)) {
                    wordsOnPage.add(s);
                    wordDisk.get(indexOfWord.get(s)).indices.add(index);
                }
            }

            }
            //System.out.println("updated page file " + pageDisk.get(index));
            //System.out.println("updated word " + wordDisk.get(index).data + " index " + index + " file " + wordDisk.get(index));
        }


    @Override
    public void rank(boolean fast) {
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            file.impact = 1.;
            file.impactTemp = 0.;
        }
        if(!fast) {int i = 0;do{i++;rankSlow();} while(i < 20);
        } else {int i = 0;do{i++;rankFast();} while(i < 20);}
    }
    void rankSlow () {
        double zeroLinkImpact = 0.;
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            if (file.indices.isEmpty()) {
                zeroLinkImpact += file.impact;
            }
        }
        zeroLinkImpact = zeroLinkImpact / pageDisk.entrySet().size();
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            InfoFile infoFile = entry.getValue();
            double impactPerIndex = infoFile.impact / infoFile.indices.size();
            for (Long index : infoFile.indices) {
                InfoFile page = pageDisk.get(index);
                 page.impactTemp += impactPerIndex;
            }
        }
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            InfoFile infoFile = entry.getValue();
            infoFile.impact = infoFile.impactTemp + zeroLinkImpact;
            infoFile.impactTemp = 0.;
        }
    }
    void rankFast (){
        double zeroLinkImpact = 0.;
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            if (file.indices.size() == 0) {
                zeroLinkImpact += file.impact;
            }
        }
        zeroLinkImpact = zeroLinkImpact / pageDisk.entrySet().size();
        try {
            PrintWriter printOut = new PrintWriter("unsorted-votes.txt");
            for(Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
                InfoFile infoFile = entry.getValue();
                double impact = infoFile.impact;
                double impactPerIndex = impact / infoFile.indices.size();
                for (Long index : infoFile.indices) {
                    Vote vote = new Vote(index, impactPerIndex);
                    printOut.println(vote);
                }
            }
            printOut.close();
        } catch (Exception e) {
            System.out.println("Call collin");
        }
        ExternalSort<Vote> sorter = new ExternalSort<Vote>(new VoteScanner());
        sorter.sort("unsorted-votes.txt","sorted-votes.txt");
        VoteScanner scanner = new VoteScanner();
        Iterator<Vote> iter = scanner.iterator("sorted-votes.txt");

        Vote vote = iter.next();
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            file.impact = zeroLinkImpact;
            while(entry.getKey().equals(vote.index)) {
                file.impact += vote.vote;
                if(iter.hasNext()) {
                    vote = iter.next();
                } else{break;}
            }
        }
    }

    @Override
    public String[] search(List<String> searchWords, int numResults) {

        String[] results = new String[numResults];
        PriorityQueue<Long> bestPageIndices = new PriorityQueue<>();
        PageIndexComparator comparator = new PageIndexComparator();
        // Iterator into list of page indices for each key word.
        Iterator<Long>[] pageIndexIterators = (Iterator<Long>[]) new Iterator[searchWords.size()];
        // Current page index in each list, just ``behind'' the iterator.

        //Write a loop to initialize the entries of pageIndexIterators.
        //pageIndexIterators[i] should be set to an iterator over the page indices in the file of searchWords[i].
        int count=0;
        for(String s:searchWords) {
            Long index = indexOfWord.get(s);
            InfoFile file = wordDisk.get(index);
            List<Long> indices = file.indices;
            pageIndexIterators[count] = indices.iterator();
            count++;
        }
        long[] currentPageIndices = new long[searchWords.size()];
        while(getNextPageIndices(currentPageIndices,pageIndexIterators)) {
            if(allEqual(currentPageIndices)) {
                InfoFile file = pageDisk.get(currentPageIndices[0]);
                System.out.println("match found " + file.data + " impact " + pageDisk.get(currentPageIndices[0]).impact);
                if(bestPageIndices.size()!=numResults) {
                    bestPageIndices.offer(currentPageIndices[0]);
                } else {
                    if(comparator.compare(file.impact,pageDisk.get(bestPageIndices.peek()).impact) > 0) {
                        System.out.println(bestPageIndices.poll());
                        bestPageIndices.offer(currentPageIndices[0]);
                    }
                }
                System.out.println(bestPageIndices);
            }
        }
        String[] temp = new String[bestPageIndices.size()];
        if(bestPageIndices.size() < numResults) {
            results = new String[bestPageIndices.size()];
        }
        int i = 0;
        while(!bestPageIndices.isEmpty()) {
            Long index = bestPageIndices.poll();
            temp[i] = pageDisk.get(index).data;
            System.out.println(pageDisk.get(index).impact);
            i++;
        }
        for(int j = temp.length-1,k=0; j >= 0; j--) {
            results[k] = temp[j];
            k++;
        }
        return results;
    }
    private boolean allEqual(long[] array) {
        for(int i = 0; i < array.length-1; i++) {
            if(array[i]!=array[i+1])
                return false;
        }
        return true;
    }
    private long getLargest(long[] array) {
        long largest = array[0];
        for (long l : array) {
            if (l > largest) {
                largest = l;
            }
        }
        return largest;
    }
    /** If all the elements of currentPageIndices are equal,
     set each one to the next() of its Iterator,
     but if any Iterator hasNext() is false, just return false.

     Otherwise, do that for every element not equal to the largest element.

     Return true.

     @param currentPageIndices array of current page indices
     @param pageIndexIterators array of iterators with next page indices
     @return true if all page indices are updated, false otherwise
     */
    private boolean getNextPageIndices (long[] currentPageIndices, Iterator<Long>[] pageIndexIterators) {
        long largest = getLargest(currentPageIndices);
        if (allEqual(currentPageIndices)) {
            for(int i = 0; i < currentPageIndices.length; i++) {
                if(!pageIndexIterators[i].hasNext()) {return false;}
                currentPageIndices[i] = pageIndexIterators[i].next();
            }
        } else {
            for(int i = 0; i < pageIndexIterators.length; i++) {
                if(currentPageIndices[i] != largest) {
                    if(pageIndexIterators[i].hasNext()) {
                        currentPageIndices[i] = pageIndexIterators[i].next();
                    } else {return false;}
                }
            }
        }
        return true;
    }
    class Vote implements Comparable<Vote> {
        Long index;
        double vote;

        public Vote (Long index, Double vote) {
            this.index = index;
            this.vote = vote;
        }

        @Override
        public int compareTo(Vote o) {
            if (this.index.compareTo(o.index) != 0) {
                return Long.compare(this.index, o.index);
            } else {
                return Double.compare(o.vote,this.vote);
            }
        }
        public String toString() {return this.index + " " + this.vote;}
    }
    class VoteScanner implements ExternalSort.EScanner<Vote> {
        class Iter implements Iterator<Vote> {
            Scanner in;

            Iter (String fileName) {
                try {
                    in = new Scanner(new File(fileName));
                } catch (Exception e) {
                    System.out.println(e);
                }
            }

            public boolean hasNext () {return in.hasNext();}

            public Vote next () {
                Long var = in.nextLong();
                double dub = in.nextDouble();
                return new Vote(var, dub);
            }
        }

        public Iterator<Vote> iterator (String fileName) {return new Iter(fileName);}
    }
    class PageIndexComparator implements Comparator<Double> {
        @Override
        public int compare(Double o1, Double o2) {
            return Double.compare(o1,o2);
        }
    }
}
