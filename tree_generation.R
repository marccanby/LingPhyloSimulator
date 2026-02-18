
### USER VALUES ###
num_trees = 16
num_taxa = 30
waitsp = 'rexp(1.0)'
waitext = 'rexp(1.0)'
output_file = 'tmp.nex'



### CODE ###
library(stringr)
library(ape)
library(TreeSimGM)
library(dispRity)


simulateTree <- function(numbsim, ntaxa, waitsp, waitexp, normage, treegen_rmvrootedge) {
  trees <- sim.taxa(as.integer(numbsim), n=as.integer(ntaxa), waitsp=waitsp, waitext = waitexp)#, m=10, gsa=TRUE)
  trees
}

postProcessSimTrees <- function(trees, normage, rmv_rootedge, trimtaxa) {
  new_trees <- list()
  for (i in 1:length(trees)) new_trees[[i]] <- trees[[i]]
  if (!rmv_rootedge) stop("Currently assumes root edge removed always......")
  if (trimtaxa) for (i in 1:length(new_trees)) new_trees[[i]] <- truncateTree(new_trees[[i]], sum(!is.na(str_extract(new_trees[[i]]$tip.label, 'sp'))))
  for (idx in 1:length(new_trees)) {
    tree <- new_trees[[idx]]
    age <- tree$age
    tree$age <- age - tree$root.edge
    tree$root.edge <- 0
    new_trees[[idx]] <- tree
  }
  if (normage) {
    for (idx in 1:length(new_trees)) {
      tree <- new_trees[[idx]]
      age <- tree$age
      tree$edge.length <- tree$edge.length / age
      tree$root.edge <- tree$root.edge / age
      tree$age <- 1
      new_trees[[idx]] <- tree
    }
  }
  new_trees
}

truncateTree <- function(tree, ntaxa) {
  if (length(tree$tip.label) == ntaxa) return(tree)
  ages <- tree.age(tree, order='present')
  ages <- ages[order(ages$ages),]
  are_leaves = grepl('sp', ages$elements) | grepl('ext', ages$elements)
  cutoff_node <- ages$elements[which(!are_leaves)[ntaxa]]
  cutoff_age <- as.numeric(ages$ages[which(!are_leaves)[ntaxa]])
  nodes_to_cut <- c()
  new_age <- 0
  for (node in ages$elements) {
    child_age <- ages[ages$elements==node,1]
    if (grepl('sp', node) || grepl('ext', node)) {
      node <- str_extract(node, '(\\d)+')
    }
    node <- as.integer(node)
    if (!(node %in% tree$edge[,2])) next
    parent <- tree$edge[which(tree$edge[,2] == node),1]
    parent_age <- ages[ages$elements==parent,1] # can never be a tip since parent
    if (parent_age >= cutoff_age) {
      nodes_to_cut <- c(nodes_to_cut, node)
    }
    else {
      if (child_age > new_age) new_age <- child_age
    }
  }
  tree2 <- tree
  new_edges <- tree$edge[!(tree$edge[,2] %in% nodes_to_cut),]
  unew_edges <-  unique(c(new_edges[,1], new_edges[,2]))
  unew_edges <- unew_edges[order(unew_edges)]
  tip_ids <- new_edges[,2][!(new_edges[,2] %in% new_edges[,1])]
  unew_edges <- c(tip_ids[order(tip_ids)], unew_edges[!(unew_edges %in% tip_ids)])
  for (i in 1:nrow(new_edges)) new_edges[i,1] <- which(unew_edges == new_edges[i,1])
  for (i in 1:nrow(new_edges)) new_edges[i,2] <- which(unew_edges == new_edges[i,2])
  tree2$edge <- new_edges
  tree2$tip.label <- paste0('t', 1:ntaxa)
  
  tree2$edge.length <- tree$edge.length[!(tree$edge[,2] %in% nodes_to_cut)]
  tree2$Nnode <- ntaxa-1
  tree2$age <- new_age  + tree$root.edge
  for (style in c('shiftsp', 'shiftext', 'shifted.sp.living', 'shifted.sp.extinct', 'shifted.ext.living', 'shifted.ext.extinct')) {
    tree2[[style]] <- NULL
    # NOTE: If keep these, the ones with living and extinct need to be udated based on which are now living and extinct
  }
  tree2
}




makeNewickString <-function(tree){
  tree<-reorder.phylo(tree,"cladewise")
  n<-length(tree$tip)
  tree$tip.label <- paste('t', seq(1,n,1), sep='')
  string<-vector(); string[1]<-"("; j<-2
  for(i in 1:nrow(tree$edge)){
    if(tree$edge[i,2]<=n){
      string[j]<-tree$tip.label[tree$edge[i,2]]; j<-j+1
      if(!is.null(tree$edge.length)){
        string[j]<-paste(c(":",round(tree$edge.length[i],10)), collapse="")
        j<-j+1
      }
      v<-which(tree$edge[,1]==tree$edge[i,1]); k<-i
      while(length(v)>0&&k==v[length(v)]){
        string[j]<-")"; j<-j+1
        w<-which(tree$edge[,2]==tree$edge[k,1])
        if(!is.null(tree$edge.length)){
          string[j]<-paste(c(":",round(tree$edge.length[w],10)), collapse="")
          j<-j+1
        }
        v<-which(tree$edge[,1]==tree$edge[w,1]); k<-w
      }
      string[j]<-","; j<-j+1
    } else if(tree$edge[i,2]>=n){
      string[j]<-"("; j<-j+1
    }
  }
  if(is.null(tree$edge.length)) string<-c(string[1:(length(string)-1)], ";")
  else string<-c(string[1:(length(string)-2)],";")
  string<-paste(string,collapse="")
  return(string)
}


trees <- simulateTree(num_trees, num_taxa, waitsp, waitext)
final_trees <- postProcessSimTrees(trees, TRUE, TRUE, TRUE)

output_str <- ''
for (idx in 1:length(final_trees)) {
  tree <- final_trees[[idx]]
  newick_str <- makeNewickString(tree)
  output_str <- paste0(output_str, newick_str)
  output_str <- paste0(output_str, '\n')
}
sink(output_file)
cat(output_str)
sink()
